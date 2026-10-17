package com.jk.explore.security.csrf;

import java.util.Map;

/** A response: status, body, and headers such as Set-Cookie. */
record Response(int status, String body, Map<String, String> headers) {

    static Response of(int status, String body) {
        return new Response(status, body, Map.of());
    }
}
