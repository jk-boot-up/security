package com.jk.explore.security.failclosedspring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the demo can move forward, so a cool-down of thirty seconds takes no time to wait for. */
final class MutableClock extends Clock {

    private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration by) {
        now = now.plus(by);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
