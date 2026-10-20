package com.jk.explore.security.tokens;

/** A status and a short message. */
public record ApiReply(int status, String message) {

    @Override
    public String toString() {
        return status + " " + message;
    }
}
