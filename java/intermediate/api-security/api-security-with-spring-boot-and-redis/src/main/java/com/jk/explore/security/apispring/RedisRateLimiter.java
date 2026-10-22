package com.jk.explore.security.apispring;

import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * The bucket kept in Redis, so every server draws on the same one. A Lua script reads the bucket, refills it from the time that has passed, takes a token and
 * writes it back in one step, which Redis runs without letting another request in between. The time comes from the caller, so the demo can move it.
 */
@Component
@ConditionalOnProperty(name = "bank.limiter", havingValue = "redis", matchIfMissing = true)
class RedisRateLimiter implements RateLimiter {

    private static final String SCRIPT = """
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local at = tonumber(redis.call('HGET', KEYS[1], 'at'))
            local now = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local perMilli = tonumber(ARGV[3])
            if tokens == nil then
              tokens = capacity
              at = now
            end
            tokens = math.min(capacity, tokens + math.max(0, now - at) * perMilli)
            local allowed = 0
            local wait = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            else
              wait = math.ceil((1 - tokens) / perMilli)
            end
            redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'at', tostring(now))
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / perMilli) * 2)
            return {allowed, wait}
            """;

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int capacity;
    private final double perMilli;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>(SCRIPT, List.class);

    RedisRateLimiter(StringRedisTemplate redis, Clock clock, @Value("${bank.budget.capacity}") int capacity, @Value("${bank.budget.refill-per-second}") double refillPerSecond) {
        this.redis = redis;
        this.clock = clock;
        this.capacity = capacity;
        this.perMilli = refillPerSecond / 1000.0;
    }

    @Override
    public Verdict take(String caller) {
        List<?> result = redis.execute(script, List.of("rate:" + caller), Long.toString(clock.millis()), Integer.toString(capacity), Double.toString(perMilli));
        if (result == null || result.size() != 2) {
            throw new IllegalStateException("the rate limiter gave no answer");
        }
        return new Verdict(((Number) result.get(0)).longValue() == 1, ((Number) result.get(1)).longValue());
    }
}
