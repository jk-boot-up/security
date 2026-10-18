package com.jk.explore.security.oidc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** A stand-in for the web inside one process: documents served at URLs. Nothing leaves the machine. */
final class Web {

    private final Map<String, Supplier<String>> documents = new HashMap<>();

    void serve(String url, Supplier<String> body) {
        documents.put(url, body);
    }

    String get(String url) throws InvalidTokenException {
        Supplier<String> body = documents.get(url);
        if (body == null) {
            throw new InvalidTokenException("nothing is served at that address");
        }
        return body.get();
    }
}
