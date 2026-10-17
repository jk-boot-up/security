package com.jk.explore.security.dns;

/** A DNS message that is too short, malformed, or built from a name that cannot be encoded. */
final class BadMessageException extends Exception {

    BadMessageException(String message) {
        super(message);
    }
}
