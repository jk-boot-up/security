package com.jk.explore.security.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A Set-Cookie header, parsed, and a judgement of whether it is safe for a login session. */
record Cookie(String name, String value, boolean secure, boolean httpOnly, String sameSite, String path, boolean hasDomain) {

    static Cookie parse(String setCookie) throws BadMessageException {
        String[] parts = setCookie.split(";");
        int equals = parts[0].indexOf('=');
        if (equals <= 0) {
            throw new BadMessageException("a cookie without a name");
        }
        String name = parts[0].substring(0, equals).strip();
        String value = parts[0].substring(equals + 1).strip();
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
                sameSite = attribute.substring(9).strip();
            } else if (lower.startsWith("path=")) {
                path = attribute.substring(5).strip();
            } else if (lower.startsWith("domain=")) {
                domain = true;
            }
        }
        return new Cookie(name, value, secure, httpOnly, sameSite, path, domain);
    }

    /** What is wrong with this cookie if it carries a login session. Empty means nothing. */
    List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (!httpOnly) {
            problems.add("no HttpOnly: a script in the page can read it");
        }
        if (!secure) {
            problems.add("no Secure: it is also sent over plain HTTP");
        }
        if (sameSite == null) {
            problems.add("no SameSite: other sites can trigger requests that carry it");
        } else if (sameSite.equalsIgnoreCase("None") && !secure) {
            problems.add("SameSite=None without Secure: browsers reject it");
        }
        if (name.startsWith("__Host-") && (!secure || !"/".equals(path) || hasDomain)) {
            problems.add("the __Host- prefix needs Secure, Path=/ and no Domain");
        }
        return problems;
    }
}
