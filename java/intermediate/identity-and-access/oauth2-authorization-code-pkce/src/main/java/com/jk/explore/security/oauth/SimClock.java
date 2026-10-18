package com.jk.explore.security.oauth;

/** A clock in whole seconds that only moves when told, so lifetimes are exact and repeatable. */
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
