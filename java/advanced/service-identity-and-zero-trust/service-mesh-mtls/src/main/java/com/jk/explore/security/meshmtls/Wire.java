package com.jk.explore.security.meshmtls;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a packet capture (tcpdump -A, the bytes printed as text) the way a person on the node would: is the request there to read? */
public final class Wire {

    private static final Pattern REQUEST = Pattern.compile("GET (\\S+) HTTP/1\\.1");
    private static final Pattern PROXY_PORT = Pattern.compile("\\.4143[ :]");

    private Wire() {
    }

    /** The first HTTP request line in the capture that carries the text, or "" when no packet shows it. */
    public static String requestLine(String capture, String text) {
        Matcher m = REQUEST.matcher(capture);
        while (m.find()) {
            if (m.group(1).contains(text)) {
                return m.group();
            }
        }
        return "";
    }

    /** Whether a packet went to or from Linkerd's inbound proxy port, 4143. */
    public static boolean proxyPort(String capture) {
        return PROXY_PORT.matcher(capture).find();
    }
}
