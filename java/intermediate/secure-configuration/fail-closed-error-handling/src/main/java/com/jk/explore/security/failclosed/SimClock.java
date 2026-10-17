package com.jk.explore.security.failclosed;

/** A clock in milliseconds that only moves when told to, so timeouts and cool-downs are exact and repeatable. */
final class SimClock {

    private long now;

    long now() {
        return now;
    }

    void advance(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("time does not go backwards");
        }
        now += millis;
    }
}
