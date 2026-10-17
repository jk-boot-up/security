package com.jk.explore.security.csrf;

import java.util.Map;

/** A request as the bank sees it: method, path, headers with lower-case names, and form or query fields. */
record Request(String method, String path, Map<String, String> headers, Map<String, String> form) {

    String header(String name) {
        return headers.get(name);
    }
}
