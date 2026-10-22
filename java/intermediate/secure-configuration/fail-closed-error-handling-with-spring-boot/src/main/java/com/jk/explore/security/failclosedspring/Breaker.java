package com.jk.explore.security.failclosedspring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/** Stops asking a service that keeps failing. While open it refuses at once, which is failing fast and still failing closed. */
final class Breaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final Clock clock;
    private final int threshold;
    private final Duration coolDown;
    private int failures;
    private Instant openedAt;
    private State state = State.CLOSED;

    Breaker(Clock clock, int threshold, Duration coolDown) {
        this.clock = clock;
        this.threshold = threshold;
        this.coolDown = coolDown;
    }

    synchronized State state() {
        if (state == State.OPEN && !clock.instant().isBefore(openedAt.plus(coolDown))) {
            state = State.HALF_OPEN;
        }
        return state;
    }

    <T> T call(Supplier<T> call) {
        if (state() == State.OPEN) {
            throw new CheckFailedException("fraud check unavailable, breaker open", "breaker open", null);
        }
        try {
            T result = call.get();
            succeeded();
            return result;
        } catch (RuntimeException e) {
            failed();
            throw e;
        }
    }

    private synchronized void succeeded() {
        failures = 0;
        state = State.CLOSED;
    }

    private synchronized void failed() {
        failures++;
        if (state == State.HALF_OPEN || failures >= threshold) {
            state = State.OPEN;
            openedAt = clock.instant();
        }
    }
}
