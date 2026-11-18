package com.jk.explore.security.apispring;

/** A budget for each caller: a bucket that holds a few requests and refills steadily. */
interface RateLimiter {

    /** {@code retryAfterMillis} is how long until one more request would be allowed; it is 0 when the request is allowed. */
    record Verdict(boolean allowed, long retryAfterMillis) {
    }

    Verdict take(String caller);

    /** The wait until the bucket holds one whole token again: the missing part divided by the refill rate, rounded up. */
    static long retryAfterMillis(double tokens, double perMilli) {
        return (long) Math.ceil((1 - tokens) / perMilli);
    }

    /** Retry-After is whole seconds, rounded up, and at least one. */
    static long retryAfterSeconds(long millis) {
        return Math.max(1, (millis + 999) / 1000);
    }
}
