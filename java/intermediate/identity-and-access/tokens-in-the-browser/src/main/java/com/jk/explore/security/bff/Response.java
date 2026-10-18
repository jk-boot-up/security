package com.jk.explore.security.bff;

/** A status and a short body. Bodies never carry tokens back to the browser. */
record Response(int status, String body) {

    static Response of(int status, String body) {
        return new Response(status, body);
    }
}
