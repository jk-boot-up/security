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

    /** True when the payment looks fraudulent: here, anything over 1000.00 at an unknown merchant. */
    boolean risky(Payment payment) throws ServiceException {
        calls++;
        switch (mode) {
            case HEALTHY -> clock.advance(40);
            case SLOW -> clock.advance(5_000);
            case BROKEN -> {
                clock.advance(10);
                throw new ServiceException("fraud-db.internal:5432 connection refused for card " + payment.card());
            }
            default -> throw new IllegalStateException();
        }
        return payment.cents() > 100_000 && payment.merchant().startsWith("unknown");
    }
}
