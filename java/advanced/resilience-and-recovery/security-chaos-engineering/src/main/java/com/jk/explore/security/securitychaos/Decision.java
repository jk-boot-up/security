package com.jk.explore.security.securitychaos;

/** What the service did with a payment, and whether the fraud service really scored it. */
record Decision(Outcome outcome, boolean screened, String reason) {

    enum Outcome { APPROVED, DENIED, REVIEW }

    boolean approved() {
        return outcome == Outcome.APPROVED;
    }

    static Decision approve(boolean screened, String reason) {
        return new Decision(Outcome.APPROVED, screened, reason);
    }

    static Decision deny(String reason) {
        return new Decision(Outcome.DENIED, false, reason);
    }

    static Decision review(String reason) {
        return new Decision(Outcome.REVIEW, false, reason);
    }
}
