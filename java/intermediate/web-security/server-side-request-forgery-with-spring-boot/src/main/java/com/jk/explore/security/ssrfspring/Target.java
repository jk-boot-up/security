package com.jk.explore.security.ssrfspring;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An address taken apart by hand, the same way for every importer. What a parser calls the host (2852039166, 127.1, [::ffff:a9fe:a9fe]) is a
 * string, not the address the network layer will connect to, so every importer here resolves the host and judges the resulting address instead.
 */
record Target(String scheme, String userInfo, String host, int port, String path) {

    private static final Pattern ADDRESS = Pattern.compile("^(?i)(https?)://([^/?#]*)([/?#].*)?$");

    static Target parse(String address) throws ImportRefused {
        Matcher m = ADDRESS.matcher(address == null ? "" : address.strip());
        if (!m.matches()) {
            throw new ImportRefused("only http and https addresses are imported");
        }
        String scheme = m.group(1).toLowerCase(java.util.Locale.ROOT);
        String authority = m.group(2);
        String userInfo = null;
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            userInfo = authority.substring(0, at);
            authority = authority.substring(at + 1);
        }
        String host = authority;
        int port = scheme.equals("https") ? 443 : 80;
        int colon = authority.lastIndexOf(':');
        if (colon > authority.lastIndexOf(']') && colon >= 0) {
            host = authority.substring(0, colon);
            try {
                port = Integer.parseInt(authority.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new ImportRefused("the port is not a number");
            }
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.isEmpty()) {
            throw new ImportRefused("the address has no host");
        }
        String path = m.group(3) == null || m.group(3).startsWith("?") ? "/" + (m.group(3) == null ? "" : m.group(3)) : m.group(3);
        return new Target(scheme, userInfo, host, port, path);
    }

    /** The address a redirect points to, relative or absolute, as a new address to be judged from the start. */
    String redirectTo(String location) {
        if (location.regionMatches(true, 0, "http://", 0, 7) || location.regionMatches(true, 0, "https://", 0, 8)) {
            return location;
        }
        String hostPart = host.contains(":") ? "[" + host + "]" : host;
        return scheme + "://" + hostPart + ":" + port + (location.startsWith("/") ? location : "/" + location);
    }
}
