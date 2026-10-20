package com.jk.explore.security.localca;

/** The certificate authority refused a request, with the reason it gives back to the requester. */
final class RefusedException extends Exception {

    RefusedException(String reason) {
        super(reason);
    }
}
