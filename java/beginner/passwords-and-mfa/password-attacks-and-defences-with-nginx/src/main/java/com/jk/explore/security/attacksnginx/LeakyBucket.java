package com.jk.explore.security.attacksnginx;

import java.util.HashMap;
import java.util.Map;

/**
 * nginx's limit_req rule, redone by hand so its steps can be seen. nginx keeps one number per key, the
 * "excess": how many requests the key is ahead of its rate. Each request adds one, the rate drains it, and a
 * request that would push it over the burst is refused with 429. Like nginx, it counts in thousandths of a request.
 */
public final class LeakyBucket {

    private final long perMinute;
    private final long burst;
    private final Map<String, Long> excess = new HashMap<>();
    private final Map<String, Long> lastMillis = new HashMap<>();

    public LeakyBucket(long perMinute, int burst) {
        this.perMinute = perMinute;
        this.burst = burst;
    }

    /**
     * One request for {@code key} at {@code millis}; true if it is let through (401 here), false for a 429.
     * A key seen for the first time starts empty. Otherwise the excess is drained by the time since the key's
     * last request, and this request adds one. A request that would take it over the burst is refused, and is
     * not counted.
     */
    public boolean allow(String key, long millis) {
        Long before = excess.get(key);
        long now = 0;
        if (before != null) {
            long drained = perMinute * 1000 * (millis - lastMillis.get(key)) / 60_000;
            now = Math.max(0, before - drained) + 1000;
        }
        if (now > burst * 1000) {
            return false;
        }
        excess.put(key, now);
        lastMillis.put(key, millis);
        return true;
    }

    /** The key's excess in whole requests, as the picture in the video shows it. */
    public long excess(String key) {
        return excess.getOrDefault(key, 0L) / 1000;
    }
}
