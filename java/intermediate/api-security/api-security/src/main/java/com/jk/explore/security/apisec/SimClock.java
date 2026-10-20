package com.jk.explore.security.apisec;

/** A clock that only moves when the demo or a test says so, in seconds, so rate limits are exact. */
public final class SimClock {

    private double seconds;

    public double now() {
        return seconds;
    }

    public void advance(double s) {
        seconds += s;
    }
}
