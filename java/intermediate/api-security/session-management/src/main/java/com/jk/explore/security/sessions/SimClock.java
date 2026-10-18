package com.jk.explore.security.sessions;

/** A clock in seconds that moves only when told, so every timeout in the demo happens at the same moment on every run. */
final class SimClock {

    private long now;

    long now() {
        return now;
    }

    void advance(long seconds) {
        now += seconds;
    }

    static long minutes(long minutes) {
        return minutes * 60;
    }
}
