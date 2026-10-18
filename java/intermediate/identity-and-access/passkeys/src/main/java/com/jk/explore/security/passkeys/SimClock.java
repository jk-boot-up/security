package com.jk.explore.security.passkeys;

/** A clock in whole seconds that only moves when told to, so challenge expiry is exact and repeatable. */
final class SimClock {

    private long now;

    long now() {
        return now;
    }

    void advance(long seconds) {
        now += seconds;
    }
}
