package com.jk.explore.security.apisec;

import java.util.HashMap;
import java.util.Map;

/** A token bucket for each caller and each kind of call: a burst up to {@code capacity}, refilled at {@code perSecond}. */
public final class RateLimiter {

    private record Bucket(double tokens, double at) {
    }

    private final SimClock clock;
    private final double capacity;
    private final double perSecond;
    private final Map<String, Bucket> buckets = new HashMap<>();

    public RateLimiter(SimClock clock, double capacity, double perSecond) {
        this.clock = clock;
        this.capacity = capacity;
        this.perSecond = perSecond;
    }

    /** Takes {@code cost} tokens, or returns how many seconds to wait; 0 means allowed. */
    public int tryTake(String key, double cost) {
        double now = clock.now();
        Bucket b = buckets.getOrDefault(key, new Bucket(capacity, now));
        double tokens = Math.min(capacity, b.tokens() + (now - b.at()) * perSecond);
        if (tokens >= cost) {
            buckets.put(key, new Bucket(tokens - cost, now));
            return 0;
        }
        buckets.put(key, new Bucket(tokens, now));
        return (int) Math.ceil((cost - tokens) / perSecond);
    }
}
