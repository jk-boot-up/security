package com.jk.explore.security.jwtnimbus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that stands still until the demo moves it, so a token's life of five minutes takes no time to wait for. */
final class SimClock extends Clock {

    static final Instant START = Instant.ofEpochSecond(1_800_000_000L);

    private volatile Instant now = START;

    void advance(Duration by) {
        now = now.plus(by);
    }

    void set(Instant to) {
        now = to;
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
