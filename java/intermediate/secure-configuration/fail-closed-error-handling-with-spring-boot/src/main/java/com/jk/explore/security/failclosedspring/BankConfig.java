package com.jk.explore.security.failclosedspring;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

@Configuration
class BankConfig {

    @Bean
    MutableClock clock() {
        return new MutableClock();
    }

    @Bean
    Breaker breaker(Clock clock, @Value("${bank.breaker.threshold}") int threshold, @Value("${bank.breaker.cool-down-seconds}") long coolDownSeconds) {
        return new Breaker(clock, threshold, Duration.ofSeconds(coolDownSeconds));
    }

    /** One retry after a failed fraud call, with no pause: a limited retry is part of the budget, an unlimited one is an attack on a struggling service. */
    @Bean
    RetryTemplate fraudRetry(@Value("${bank.fraud.attempts}") long attempts) {
        return new RetryTemplate(RetryPolicy.builder().maxRetries(attempts - 1).delay(Duration.ZERO).build());
    }
}
