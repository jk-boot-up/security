package com.jk.explore.security.securitylogging;

import java.time.Duration;
import java.time.Instant;

/** A clock that moves only when told to, so every timestamp in the demo is the same on every run. */
final class SimClock {

    static final Instant START = Instant.parse("2026-10-18T09:00:00Z");

    private Instant now;

    SimClock() {
        this(START);
    }

    SimClock(Instant start) {
        this.now = start;
    }

    Instant now() {
        return now;
    }

    void advance(Duration step) {
        now = now.plus(step);
    }
}
