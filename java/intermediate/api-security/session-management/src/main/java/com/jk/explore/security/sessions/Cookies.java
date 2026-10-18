package com.jk.explore.security.sessions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The session cookie, written with every attribute that protects it, and a check that lists what a Set-Cookie header is missing. */
final class Cookies {

    static final String NAME = "__Host-session";

    private Cookies() {
    }

    static String sessionCookie(String id) {
        return NAME + "=" + id + "; Path=/; Secure; HttpOnly; SameSite=Strict";
    }

    static String forget() {
        return NAME + "=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0";
    }

    /** What is wrong with a Set-Cookie header that carries a session. Empty means nothing. */
    static List<String> problems(String setCookie) {
        String[] parts = setCookie.split(";");
        String name = parts[0].substring(0, Math.max(0, parts[0].indexOf('='))).strip();
        boolean secure = false;
        boolean httpOnly = false;
        boolean domain = false;
        String sameSite = null;
        String path = null;
        for (int i = 1; i < parts.length; i++) {
            String attribute = parts[i].strip();
            String lower = attribute.toLowerCase(Locale.ROOT);
            if (lower.equals("secure")) {
                secure = true;
            } else if (lower.equals("httponly")) {
                httpOnly = true;
            } else if (lower.startsWith("samesite=")) {
                sameSite = attribute.substring(9).strip().toLowerCase(Locale.ROOT);
            } else if (lower.startsWith("path=")) {
                path = attribute.substring(5).strip();
            } else if (lower.startsWith("domain=")) {
                domain = true;
            }
        }
        List<String> problems = new ArrayList<>();
        if (!secure) {
            problems.add("no Secure");
        }
        if (!httpOnly) {
            problems.add("no HttpOnly");
        }
        if (!"strict".equals(sameSite) && !"lax".equals(sameSite)) {
            problems.add("no SameSite");
        }
        if (!name.startsWith("__Host-")) {
            problems.add("no __Host- prefix");
        } else if (!"/".equals(path) || domain) {
            problems.add("__Host- needs Path=/ and no Domain");
        }
        return problems;
    }
}
