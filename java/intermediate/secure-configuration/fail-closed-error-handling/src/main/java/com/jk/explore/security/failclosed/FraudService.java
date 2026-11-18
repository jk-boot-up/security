package com.jk.explore.security.failclosed;

/**
 * The fraud-check service, simulated. Each call takes some time on the simulated clock. It can be healthy, slow, or broken, and a broken
 * one fails with an internal message, the kind a real dependency puts in its exceptions.
 */
final class FraudService {

    enum Mode { HEALTHY, SLOW, BROKEN }

    private final SimClock clock;
    private Mode mode;
    private int calls;

    FraudService(SimClock clock, Mode mode) {
        this.clock = clock;
        this.mode = mode;
    }

    void setMode(Mode mode) {
        this.mode = mode;
    }

    int calls() {
        return calls;
    }

    /** True when the payment looks fraudulent: here, anything over 1000.00 at an unknown merchant. Waits as long as it takes. */
    boolean risky(Payment payment) throws ServiceException {
        return risky(payment, Long.MAX_VALUE);
    }

    /** The same call with a time budget: a call that needs longer is given up when the budget runs out, as a client timeout does. */
    boolean risky(Payment payment, long budgetMillis) throws ServiceException {
        calls++;
        long needs = millisNeeded();
        if (needs > budgetMillis) {
            clock.advance(budgetMillis);
            throw new TimeoutException("fraud check", budgetMillis);
        }
        clock.advance(needs);
        if (mode == Mode.BROKEN) {
            throw new ServiceException("fraud-db.internal:5432 connection refused for card " + payment.card());
        }
        return payment.cents() > 100_000 && payment.merchant().startsWith("unknown");
    }

    /** How long one call takes in each mode. */
    long millisNeeded() {
        return switch (mode) {
            case HEALTHY -> 40;
            case SLOW -> 5_000;
            case BROKEN -> 10;
        };
    }
}
