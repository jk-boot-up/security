package com.jk.explore.security.apispring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that stands still until the demo moves it, so a thousand requests all happen "at the same moment" and a refill takes no time to wait for. */
final class ManualClock extends Clock {

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
