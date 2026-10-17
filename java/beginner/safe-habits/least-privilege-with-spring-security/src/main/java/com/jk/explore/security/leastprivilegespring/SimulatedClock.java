package com.jk.explore.security.leastprivilegespring;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that moves only when the demo says so: tick N is N seconds after the epoch. */
final class SimulatedClock extends Clock {

    private volatile long tick;

    void set(long tick) {
        this.tick = tick;
    }

    long tick() {
        return tick;
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
        return Instant.EPOCH.plusSeconds(tick);
    }
}
