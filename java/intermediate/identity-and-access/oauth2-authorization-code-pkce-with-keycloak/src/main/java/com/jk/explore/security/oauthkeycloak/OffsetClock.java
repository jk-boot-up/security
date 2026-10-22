package com.jk.explore.security.oauthkeycloak;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The system clock plus an offset the demo can move forward, so a token's five minutes take no time to wait for. */
final class OffsetClock extends Clock {

    private volatile Duration offset = Duration.ZERO;

    void advance(Duration by) {
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
