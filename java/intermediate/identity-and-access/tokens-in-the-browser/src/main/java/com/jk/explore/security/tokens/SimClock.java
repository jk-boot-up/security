package com.jk.explore.security.tokens;

/** A clock in whole seconds that only moves when the demo or a test says so. */
public final class SimClock {

    private long seconds = 1_700_000_000L;

    public long now() {
        return seconds;
    }

    public void advance(long s) {
        seconds += s;
    }
}
