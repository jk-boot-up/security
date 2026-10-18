package com.jk.explore.security.bff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A model browser holding one banking page. It keeps what a real browser keeps (localStorage, cookies, a non-extractable key) and
 * applies the rules that matter here: scripts on the page can read localStorage and non-HttpOnly cookies, but not HttpOnly cookies or
 * the private key; SameSite decides whether cookies go with requests started by another site; and a request from another site that
 * adds a custom header is first asked about with a CORS preflight.
 */
final class Browser {

    static final String BANK_SITE = "bank.example";

    /** A modern browser applies SameSite; an older one sends cookies with every request, whoever started it. */
    enum Kind { MODERN, OLDER }

    private final Kind kind;
    private final Bff bff;
    private final BankApi api;
    private final Map<String, String> localStorage = new LinkedHashMap<>();
    private final Map<String, Cookie> cookies = new TreeMap<>();
    private DpopKey key;
    private boolean pageOpen = true;

    Browser(Kind kind, Bff bff, BankApi api) {
        this.kind = kind;
        this.bff = bff;
        this.api = api;
    }

    void store(String name, String value) {
        localStorage.put(name, value);
    }

    String stored(String name) {
        return localStorage.get(name);
    }

    void clearStorage() {
        localStorage.clear();
        key = null;
    }

    void receiveCookie(String setCookie) {
        Cookie cookie = Cookie.parse(setCookie);
        cookies.put(cookie.name(), cookie);
    }

    Cookie cookie(String name) {
        return cookies.get(name);
    }

    void holdKey(DpopKey key) {
        this.key = key;
    }

    /** The page may ask the key for a proof; it never gets the private key itself. */
    DpopKey keyForThisPage() {
        if (!pageOpen) {
            throw new IllegalStateException("the page is closed");
        }
        return key;
    }

    boolean pageOpen() {
        return pageOpen;
    }

    void closePage() {
        pageOpen = false;
    }

    /** Everything a script running on the page can read: localStorage entries and cookies without HttpOnly. */
    Map<String, String> readableByScripts() {
        Map<String, String> view = new LinkedHashMap<>(localStorage);
        cookies.values().stream().filter(c -> !c.httpOnly()).forEach(c -> view.put("cookie " + c.name(), c.value()));
        return view;
    }

    /**
     * Sends a request started by a page on {@code fromSite}. Requests to the BFF carry the cookies the rules allow; requests to the
     * API carry only what the page put on them. A cross-site request with a custom header needs a preflight the BFF refuses.
     */
    Response send(String fromSite, Request request) {
        boolean crossSite = !fromSite.equals(BANK_SITE);
        if (request.url().startsWith(Bff.ORIGIN)) {
            if (crossSite && request.header(Bff.REQUEST_HEADER) != null && !bff.allowsCrossOrigin("https://" + fromSite)) {
                return Response.of(0, "never sent: the browser's preflight was refused");
            }
            Map<String, String> jar = new TreeMap<>();
            for (Cookie cookie : cookies.values()) {
                boolean sameSiteAllows = !crossSite || cookie.sameSite().equalsIgnoreCase("None");
                if (sameSiteAllows || kind == Kind.OLDER) {
                    jar.put(cookie.name(), cookie.value());
                }
            }
            return bff.handle(request.withCookies(jar));
        }
        return api.handle(request);
    }
}
