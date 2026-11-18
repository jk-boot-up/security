package com.jk.explore.security.cors;

import java.net.URI;

/**
 * An origin is three parts of a web address: the scheme, the host and the port. Two pages have the same origin only when all
 * three are equal; the path does not count. A missing port is the scheme's default (443 for https, 80 for http).
 */
public record Origin(String scheme, String host, int port) {

    public static Origin parse(String text) {
        URI u = URI.create(text);
        int port = u.getPort() != -1 ? u.getPort() : u.getScheme().equals("https") ? 443 : 80;
        return new Origin(u.getScheme(), u.getHost(), port);
    }

    public boolean sameAs(Origin other) {
        return scheme.equals(other.scheme) && host.equals(other.host) && port == other.port;
    }
}
