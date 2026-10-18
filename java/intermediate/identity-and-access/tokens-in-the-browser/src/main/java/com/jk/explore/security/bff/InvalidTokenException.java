package com.jk.explore.security.bff;

/** A token the validator refuses. The message says which check failed; it never contains the token. */
final class InvalidTokenException extends Exception {

    InvalidTokenException(String reason) {
        super(reason);
    }
}
