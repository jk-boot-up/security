package com.jk.explore.security.webhooks;

/** What the store's webhook endpoint answers: an HTTP status and a short message. */
record Reply(int status, String message) {

    @Override
    public String toString() {
        return status + " " + message;
    }
}
