package com.jk.explore.security.ssrf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A web address taken apart by hand, so each part can be checked: scheme, user info, host, port and path. */
public record Url(String scheme, String userInfo, String host, int port, String path) {

    private static final Pattern PARTS = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*)://([^/?#]*)([^#]*)$");

    public static Url parse(String text) {
        Matcher m = PARTS.matcher(text == null ? "" : text.trim());
        if (!m.matches()) {
            throw new ImportRefused("not a web address");
        }
        String scheme = m.group(1).toLowerCase(java.util.Locale.ROOT);
        String authority = m.group(2);
        String path = m.group(3).isEmpty() ? "/" : m.group(3);
        String userInfo = null;
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            userInfo = authority.substring(0, at);
            authority = authority.substring(at + 1);
        }
        String host = authority;
        int port = scheme.equals("https") ? 443 : 80;
        int colon = authority.lastIndexOf(':');
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            if (close < 0) {
                throw new ImportRefused("not a web address");
            }
            host = authority.substring(1, close);
            if (authority.length() > close + 1 && authority.charAt(close + 1) == ':') {
                port = number(authority.substring(close + 2));
            }
        } else if (colon >= 0) {
            host = authority.substring(0, colon);
            port = number(authority.substring(colon + 1));
        }
        if (host.isEmpty()) {
            throw new ImportRefused("not a web address");
        }
        return new Url(scheme, userInfo, host.toLowerCase(java.util.Locale.ROOT), port, path);
    }

    private static int number(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ImportRefused("not a web address");
        }
    }

    /** A redirect target: absolute addresses stand alone, a path is read against this address. */
    public Url resolve(String location) {
        if (location.startsWith("/")) {
            return new Url(scheme, userInfo, host, port, location);
        }
        return parse(location);
    }
}
