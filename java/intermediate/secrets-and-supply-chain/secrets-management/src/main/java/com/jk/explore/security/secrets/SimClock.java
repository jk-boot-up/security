package com.jk.explore.security.secrets;

/** A clock in seconds that moves only when told to, so leases and rotations are the same on every run. */
final class SimClock {

    private long now;

    long now() {
        return now;
    }

    void advance(long seconds) {
        now += seconds;
    }
}
