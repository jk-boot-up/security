package com.jk.explore.security.jwtnimbus;

/** A token the API will not accept, with the reason the library gave. */
final class Refused extends Exception {

    Refused(String reason, Throwable cause) {
        super(reason, cause);
    }

    Refused(String reason) {
        super(reason);
    }
}
