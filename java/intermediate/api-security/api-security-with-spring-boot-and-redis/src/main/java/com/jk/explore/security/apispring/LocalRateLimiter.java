package com.jk.explore.security.apispring;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** The same bucket kept in this server's memory. Each server has its own, so two servers give a caller twice the budget. */
@Component
@ConditionalOnProperty(name = "bank.limiter", havingValue = "local")
class LocalRateLimiter implements RateLimiter {

    private record Bucket(double tokens, long at) {
    }

    private final Clock clock;
    private final double capacity;
    private final double perMilli;
    private final Map<String, Bucket> buckets = new HashMap<>();

    LocalRateLimiter(Clock clock, @Value("${bank.budget.capacity}") int capacity, @Value("${bank.budget.refill-per-second}") double refillPerSecond) {
        this.clock = clock;
        this.capacity = capacity;
        this.perMilli = refillPerSecond / 1000.0;
    }

    @Override
    public synchronized Verdict take(String caller) {
        long now = clock.millis();
        Bucket b = buckets.getOrDefault(caller, new Bucket(capacity, now));
        double tokens = Math.min(capacity, b.tokens() + Math.max(0, now - b.at()) * perMilli);
        if (tokens >= 1) {
            buckets.put(caller, new Bucket(tokens - 1, now));
            return new Verdict(true, 0);
        }
        buckets.put(caller, new Bucket(tokens, now));
        return new Verdict(false, (long) Math.ceil((1 - tokens) / perMilli));
    }
}
