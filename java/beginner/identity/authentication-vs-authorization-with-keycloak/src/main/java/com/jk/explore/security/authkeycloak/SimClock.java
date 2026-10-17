package com.jk.explore.security.authkeycloak;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that only moves when told to, so token lifetimes can be shown without waiting. */
final class SimClock extends Clock {

    private Instant now;

    SimClock(Instant start) {
        this.now = start;
    }

    void advanceSeconds(long seconds) {
        now = now.plusSeconds(seconds);
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
