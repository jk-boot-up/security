package com.jk.explore.security.cors;

import java.util.Map;

/** A request as the bank API sees it: method, path, headers with lower-case names, and whether the browser attached the session cookie. */
public record Request(String method, String path, Map<String, String> headers, boolean hasSessionCookie) {

    public String header(String name) {
        return headers.get(name.toLowerCase(java.util.Locale.ROOT));
    }
}
