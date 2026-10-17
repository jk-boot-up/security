package com.jk.explore.security.failclosed;

/**
 * A circuit breaker that fails closed. After a run of failures it opens, and while it is open every call is refused at once instead
 * of waiting for a timeout. After the cool-down it lets one trial call through; success closes it again.
 */
final class CircuitBreaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final SimClock clock;
    private final int failuresToOpen;
    private final long coolDownMillis;
    private State state = State.CLOSED;
    private int failures;
    private long openedAt;

    CircuitBreaker(SimClock clock, int failuresToOpen, long coolDownMillis) {
        this.clock = clock;
        this.failuresToOpen = failuresToOpen;
        this.coolDownMillis = coolDownMillis;
    }

    /** Whether a call may be tried now. An open breaker answers no, and so the payment is refused. */
    boolean allowsCall() {
        if (state == State.OPEN && clock.now() - openedAt >= coolDownMillis) {
            state = State.HALF_OPEN;
        }
        return state != State.OPEN;
    }

    void success() {
        state = State.CLOSED;
        failures = 0;
    }

    void failure() {
        failures++;
        if (state == State.HALF_OPEN || failures >= failuresToOpen) {
            state = State.OPEN;
            openedAt = clock.now();
        }
    }

    State state() {
        return state;
    }
}
