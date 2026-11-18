package com.jk.explore.security.searchdetect;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Redoes by hand what a {@code date_histogram} with a {@code fixed_interval} does: each event falls in the bucket that starts at its time rounded down to a whole
 * interval since 1970. The buckets never move, so a burst that crosses a boundary is split in two.
 */
public final class Buckets {

    private Buckets() {
    }

    public static Instant start(Instant time, Duration interval) {
        long ms = interval.toMillis();
        return Instant.ofEpochMilli(Math.floorDiv(time.toEpochMilli(), ms) * ms);
    }

    /** How many events fall in each fixed bucket, by the bucket's start. */
    public static Map<Instant, Long> fixed(List<Event> events, Duration interval) {
        Map<Instant, Long> counts = new TreeMap<>();
        events.forEach(e -> counts.merge(start(e.time(), interval), 1L, Long::sum));
        return counts;
    }
}
