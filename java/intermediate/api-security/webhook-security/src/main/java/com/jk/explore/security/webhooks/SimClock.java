package com.jk.explore.security.webhooks;

/** A clock in whole seconds that only moves when told, so every timestamp in the demo is the same on every run. */
final class SimClock {

    private long now;

    SimClock(long start) {
        this.now = start;
    }

    long now() {
        return now;
    }

    void advance(long seconds) {
        now += seconds;
    }
}
