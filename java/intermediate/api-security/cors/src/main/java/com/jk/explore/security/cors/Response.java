package com.jk.explore.security.cors;

import java.util.LinkedHashMap;
import java.util.Map;

/** A response: status, headers with lower-case names, and a body. */
public record Response(int status, Map<String, String> headers, String body) {

    public static Response of(int status, String body) {
        return new Response(status, new LinkedHashMap<>(), body);
    }

    public Response with(String name, String value) {
        Map<String, String> h = new LinkedHashMap<>(headers);
        h.put(name.toLowerCase(java.util.Locale.ROOT), value);
        return new Response(status, h, body);
    }

    public String header(String name) {
        return headers.get(name.toLowerCase(java.util.Locale.ROOT));
    }
}
