package com.jk.explore.security.oauth;

/** An OAuth 2.0 error response: an error code from RFC 6749 and a human-readable description. */
final class OAuthException extends Exception {

    private final String error;

    OAuthException(String error, String description) {
        super(description);
        this.error = error;
    }

    String error() {
        return error;
    }

    /** How the demo prints a refusal: the error code and its reason. */
    String shown() {
        return error + ": " + getMessage();
    }
}
