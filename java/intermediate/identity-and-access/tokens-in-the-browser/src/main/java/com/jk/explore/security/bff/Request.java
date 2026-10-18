package com.jk.explore.security.bff;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** An HTTP request as the servers in this demo see it: method, full URL, headers (names compared without case) and cookies. */
record Request(String method, String url, Map<String, String> headers, Map<String, String> cookies) {

    Request {
        Map<String, String> lower = new TreeMap<>();
        headers.forEach((k, v) -> lower.put(k.toLowerCase(Locale.ROOT), v));
        headers = Map.copyOf(lower);
        cookies = Map.copyOf(cookies);
    }

    static Request of(String method, String url) {
        return new Request(method, url, Map.of(), Map.of());
    }

    Request withHeader(String name, String value) {
        Map<String, String> copy = new TreeMap<>(headers);
        copy.put(name.toLowerCase(Locale.ROOT), value);
        return new Request(method, url, copy, cookies);
    }

    Request withCookies(Map<String, String> jar) {
        return new Request(method, url, headers, jar);
    }

    String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    boolean changesState() {
        return !method.equals("GET") && !method.equals("HEAD");
    }
}
