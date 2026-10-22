package com.jk.explore.security.apispring;

/** A budget for each caller: a bucket that holds a few requests and refills steadily. */
interface RateLimiter {

    /** {@code retryAfterMillis} is how long until one more request would be allowed; it is 0 when the request is allowed. */
    record Verdict(boolean allowed, long retryAfterMillis) {
    }

    Verdict take(String caller);
}
