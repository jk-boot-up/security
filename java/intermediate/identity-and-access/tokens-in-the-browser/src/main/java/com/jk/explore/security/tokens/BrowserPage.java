package com.jk.explore.security.tokens;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a script running <em>on the page</em> can reach, which is the whole point: web storage is readable by every script on the page (including an injected one),
 * {@code document.cookie} shows cookies that are not HttpOnly, and an HttpOnly cookie can be used by the page (the browser attaches it) but never read.
 */
public final class BrowserPage {

    private final Map<String, String> localStorage = new LinkedHashMap<>();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final Map<String, Boolean> httpOnly = new LinkedHashMap<>();

    public void storeInLocalStorage(String key, String value) {
        localStorage.put(key, value);
    }

    public void setCookie(String name, String value, boolean httpOnlyFlag) {
        cookies.put(name, value);
        httpOnly.put(name, httpOnlyFlag);
    }

    /** What an injected script can read and send to its owner. */
    public Map<String, String> whatAnInjectedScriptCanSteal() {
        Map<String, String> loot = new LinkedHashMap<>();
        localStorage.forEach((k, v) -> loot.put("localStorage." + k, v));
        cookies.forEach((k, v) -> {
            if (!httpOnly.get(k)) {
                loot.put("cookie." + k, v);
            }
        });
        return loot;
    }

    /** What the browser attaches to a request to the site, whoever started the request. */
    public String cookieHeader() {
        StringBuilder b = new StringBuilder();
        cookies.forEach((k, v) -> b.append(b.length() == 0 ? "" : "; ").append(k).append('=').append(v));
        return b.toString();
    }
}
