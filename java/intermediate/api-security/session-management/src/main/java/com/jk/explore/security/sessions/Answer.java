package com.jk.explore.security.sessions;

/** What the bank answers to a request that carries a session id. */
enum Answer {
    ALLOWED("allowed"),
    UNKNOWN("refused: unknown session"),
    IDLE_EXPIRED("refused: idle too long, sign in again"),
    ABSOLUTE_EXPIRED("refused: session too old, sign in again"),
    NOT_SIGNED_IN("refused: not signed in"),
    REAUTH_REQUIRED("asks for the password again");

    private final String text;

    Answer(String text) {
        this.text = text;
    }

    String text() {
        return text;
    }
}
