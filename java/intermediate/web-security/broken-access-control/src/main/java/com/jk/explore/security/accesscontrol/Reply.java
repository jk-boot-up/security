package com.jk.explore.security.accesscontrol;

/** The store's answer: an HTTP status and a short body. */
record Reply(int status, String body) {

    static Reply ok(String body) {
        return new Reply(200, body);
    }

    static Reply status(int status, String body) {
        return new Reply(status, body);
    }

    boolean succeeded() {
        return status >= 200 && status < 300;
    }
}
