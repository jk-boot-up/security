package com.jk.explore.security.cors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A model of the browser rules that matter: a page may <em>send</em> some cross-origin requests without asking, may <em>read</em> the answer only when the
 * server names the page's origin (and, with cookies, also says credentials are allowed), and asks first with a preflight for anything that is not simple.
 */
public final class Browser {

    private static final Set<String> SIMPLE_METHODS = Set.of("GET", "HEAD", "POST");
    private static final Set<String> SIMPLE_TYPES = Set.of("text/plain", "application/x-www-form-urlencoded", "multipart/form-data");

    private final BankApi bank;

    public Browser(BankApi bank) {
        this.bank = bank;
    }

    /**
     * A script on {@code pageOrigin} calls the bank. {@code origin} is what the browser puts in the Origin header ({@code "null"} for sandboxed frames).
     * The session cookie is attached when {@code withCredentials} is set, as it would be for a SameSite=None cookie.
     */
    public FetchResult fetch(String pageOrigin, String method, String path, String contentType, boolean withCredentials) {
        if (pageOrigin.equals(BankApi.ORIGIN)) {
            Response r = bank.handle(new Request(method, path, Map.of(), true)); // same origin: no CORS at all
            return new FetchResult(true, true, r.body());
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("origin", pageOrigin);
        if (contentType != null) {
            headers.put("content-type", contentType);
        }
        boolean simple = SIMPLE_METHODS.contains(method) && (contentType == null || SIMPLE_TYPES.contains(contentType));
        if (!simple) {
            Map<String, String> pre = new HashMap<>();
            pre.put("origin", pageOrigin);
            pre.put("access-control-request-method", method);
            if (contentType != null) {
                pre.put("access-control-request-headers", "content-type");
            }
            Response p = bank.handle(new Request("OPTIONS", path, pre, false)); // never carries the cookie
            if (!approves(p, pageOrigin, withCredentials) || !allowed(p.header("access-control-allow-methods"), method)
                    || (contentType != null && !allowed(p.header("access-control-allow-headers"), "content-type"))) {
                return new FetchResult(false, false, null);
            }
        }
        Response r = bank.handle(new Request(method, path, headers, withCredentials));
        boolean readable = approves(r, pageOrigin, withCredentials);
        return new FetchResult(true, readable, readable ? r.body() : null);
    }

    /** The browser's check on a response: Access-Control-Allow-Origin must name the page (or be {@code *} without credentials). */
    static boolean approves(Response r, String pageOrigin, boolean withCredentials) {
        String allow = r.header("access-control-allow-origin");
        if (allow == null) {
            return false;
        }
        if (withCredentials) {
            return allow.equals(pageOrigin) && "true".equals(r.header("access-control-allow-credentials")); // "*" is refused when cookies are involved
        }
        return allow.equals("*") || allow.equals(pageOrigin);
    }

    private static boolean allowed(String list, String item) {
        if (list == null) {
            return false;
        }
        for (String s : list.split(",")) {
            if (s.trim().equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
}
