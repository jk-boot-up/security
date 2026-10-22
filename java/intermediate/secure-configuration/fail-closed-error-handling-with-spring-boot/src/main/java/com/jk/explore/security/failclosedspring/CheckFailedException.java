package com.jk.explore.security.failclosedspring;

/** A check could not be made. The message is for the log; it names the card and the server on purpose, so the lesson can show a response that leaks it. */
class CheckFailedException extends RuntimeException {

    final String why;

    CheckFailedException(String why, String message, Throwable cause) {
        super(message, cause);
        this.why = why;
    }
}
