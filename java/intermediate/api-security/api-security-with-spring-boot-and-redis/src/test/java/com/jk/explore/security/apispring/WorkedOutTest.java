package com.jk.explore.security.apispring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The values the demo works out by hand, checked without Docker or a server. */
class WorkedOutTest {

    @Test
    void retryAfterIsTheMissingTokenOverTheRefillRateRoundedUp() {
        assertEquals(1000, RateLimiter.retryAfterMillis(0, 0.001));
        assertEquals(500, RateLimiter.retryAfterMillis(0.5, 0.001));
        assertEquals(1, RateLimiter.retryAfterSeconds(1000));
        assertEquals(2, RateLimiter.retryAfterSeconds(1001));
        assertEquals(1, RateLimiter.retryAfterSeconds(1));
    }

    @Test
    void theModelLimitsAreReadFromTheAnnotations() {
        assertEquals(List.of("displayName       1 to 40 characters", "email             an e-mail address, at most 80 characters"),
                ApiSpringDemo.modelLimits());
    }

    @Test
    void theLocalBucketAgreesWithTheWorkedOutWait() {
        ManualClock clock = new ManualClock();
        LocalRateLimiter limiter = new LocalRateLimiter(clock, 2, 1);
        limiter.take("ana");
        limiter.take("ana");
        RateLimiter.Verdict refused = limiter.take("ana");
        assertEquals(false, refused.allowed());
        assertEquals(RateLimiter.retryAfterMillis(0, 0.001), refused.retryAfterMillis());
    }
}
