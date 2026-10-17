package com.jk.explore.security.failclosed;

/**
 * How payments get approved by accident. The decision starts as "allowed" and a check can only turn it off; when the check throws,
 * the catch-all swallows the error, nothing turns the decision off, and the payment goes through. The limit check has the same shape.
 */
final class NaiveAuthorizer {

    private final FraudService fraud;
    private final long dailyLimitCents;

    NaiveAuthorizer(FraudService fraud, long dailyLimitCents) {
        this.fraud = fraud;
        this.dailyLimitCents = dailyLimitCents;
    }

    Decision authorize(Payment payment, String limitText) {
        boolean allowed = true;
        try {
            if (fraud.risky(payment)) {
                allowed = false;
            }
        } catch (Exception e) {
            // "the fraud check is flaky, don't block customers": the bug this project is about
        }
        try {
            long spentToday = Long.parseLong(limitText);
            if (spentToday + payment.cents() > dailyLimitCents) {
                allowed = false;
            }
        } catch (RuntimeException e) {
            // a bad value in the account record skips the limit check entirely
        }
        return allowed ? Decision.approve("-") : Decision.refuse("refused", "-");
    }
}
