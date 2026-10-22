package com.jk.explore.security.bffspring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The system clock plus an offset the demo can move forward, so minutes and days take no time to wait for. */
public final class OffsetClock extends Clock {

    private volatile Duration offset = Duration.ZERO;

    public void advance(Duration by) {
        offset = offset.plus(by);
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
        return Instant.now().plus(offset);
    }
}
