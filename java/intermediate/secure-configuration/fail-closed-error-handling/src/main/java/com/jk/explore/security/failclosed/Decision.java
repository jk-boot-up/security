package com.jk.explore.security.failclosed;

/** The answer to a payment: approved or refused, with the reason and the reference the customer can quote. */
record Decision(boolean approved, String reason, String reference) {

    static Decision approve(String reference) {
        return new Decision(true, "approved", reference);
    }

    static Decision refuse(String reason, String reference) {
        return new Decision(false, reason, reference);
    }
}
