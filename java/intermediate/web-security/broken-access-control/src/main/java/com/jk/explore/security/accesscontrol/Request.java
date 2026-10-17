package com.jk.explore.security.accesscontrol;

/** One request to the store: a method, a path, the bearer token (which may be missing), and a short body. */
record Request(String method, String path, String token, String body) {

    static Request of(String method, String path, String token) {
        return new Request(method, path, token, "");
    }

    static Request of(String method, String path, String token, String body) {
        return new Request(method, path, token, body);
    }

    @Override
    public String toString() {
        return method + " " + path;
    }
}
