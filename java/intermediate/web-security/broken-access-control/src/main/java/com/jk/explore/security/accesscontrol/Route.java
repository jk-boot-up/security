package com.jk.explore.security.accesscontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A method and a path pattern such as /orders/{id}. A {name} segment matches one path segment. */
record Route(String method, String pattern) {

    /** The values of the {name} segments if the request matches, or nothing. */
    Optional<Map<String, String>> match(Request request) {
        if (!method.equals(request.method())) {
            return Optional.empty();
        }
        String[] want = pattern.split("/", -1);
        String[] got = request.path().split("/", -1);
        if (want.length != got.length) {
            return Optional.empty();
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < want.length; i++) {
            if (want[i].startsWith("{") && want[i].endsWith("}")) {
                if (got[i].isEmpty()) {
                    return Optional.empty();
                }
                values.put(want[i].substring(1, want[i].length() - 1), got[i]);
            } else if (!want[i].equals(got[i])) {
                return Optional.empty();
            }
        }
        return Optional.of(values);
    }

    @Override
    public String toString() {
        return method + " " + pattern;
    }
}
