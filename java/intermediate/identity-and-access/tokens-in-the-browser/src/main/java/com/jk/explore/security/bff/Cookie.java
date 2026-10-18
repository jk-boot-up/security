package com.jk.explore.security.bff;

import java.util.Locale;

/** A cookie as the browser stores it, with the attributes that decide who can read it and when it is sent. */
record Cookie(String name, String value, boolean secure, boolean httpOnly, String sameSite, String path, boolean hostOnly) {

    /** Parses a Set-Cookie header value. Attributes are matched without case; unknown attributes are ignored, as browsers do. */
    static Cookie parse(String setCookie) {
        String[] parts = setCookie.split(";");
        int eq = parts[0].indexOf('=');
        String name = parts[0].substring(0, eq).strip();
        String value = parts[0].substring(eq + 1).strip();
        boolean secure = false;
        boolean httpOnly = false;
        boolean hasDomain = false;
        String sameSite = "Lax";
        String path = null;
        for (int i = 1; i < parts.length; i++) {
            String attribute = parts[i].strip();
            String lower = attribute.toLowerCase(Locale.ROOT);
            if (lower.equals("secure")) {
                secure = true;
            } else if (lower.equals("httponly")) {
                httpOnly = true;
            } else if (lower.startsWith("samesite=")) {
                sameSite = attribute.substring(9).strip();
            } else if (lower.startsWith("path=")) {
                path = attribute.substring(5).strip();
            } else if (lower.startsWith("domain=")) {
                hasDomain = true;
            }
        }
        return new Cookie(name, value, secure, httpOnly, sameSite, path, !hasDomain);
    }

    /** The attributes as one line, without the value, for printing. */
    String describe() {
        return name + "; Path=" + path + (secure ? "; Secure" : "") + (httpOnly ? "; HttpOnly" : "") + "; SameSite=" + sameSite;
    }

    /** The __Host- prefix rules (RFC 6265bis): Secure, Path=/ and no Domain, so no other host can set or overwrite the cookie. */
    boolean hostPrefixValid() {
        return !name.startsWith("__Host-") || (secure && "/".equals(path) && hostOnly);
    }
}
