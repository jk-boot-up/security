package com.jk.explore.security.http;

/** A request, response, header or cookie that is malformed or ambiguous, and so is refused. */
final class BadMessageException extends Exception {

    BadMessageException(String message) {
        super(message);
    }
}
