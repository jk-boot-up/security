package com.jk.explore.security.failclosed;

import java.util.Map;
import java.util.TreeMap;

/**
 * Failing closed without failing slowly: a time budget per call, at most one retry for a call that failed fast, a circuit breaker
 * that refuses at once when the fraud service is down, and a count of refusals by reason so that someone is alerted.
 */
final class ResilientAuthorizer {

    private final FraudService fraud;
    private final SimClock clock;
    private final CircuitBreaker breaker;
    private final int alertAfterRefusals;
    private final Map<String, Integer> refusals = new TreeMap<>();
    private boolean alerted;

    ResilientAuthorizer(FraudService fraud, SimClock clock, CircuitBreaker breaker, int alertAfterRefusals) {
        this.fraud = fraud;
        this.clock = clock;
        this.breaker = breaker;
        this.alertAfterRefusals = alertAfterRefusals;
    }

    Decision authorize(Payment payment) {
        if (!breaker.allowsCall()) {
            return refuse("fraud check unavailable, breaker open");
        }
        for (int attempt = 1; attempt <= 2; attempt++) {
            long start = clock.now();
            try {
                boolean risky = fraud.risky(payment);
                if (clock.now() - start > SafeAuthorizer.FRAUD_BUDGET_MS) {
                    breaker.failure();
                    return refuse("fraud check too slow");
                }
                breaker.success();
                return risky ? refuse("looks fraudulent") : Decision.approve("-");
            } catch (ServiceException e) {
                breaker.failure();
                if (!breaker.allowsCall()) {
                    return refuse("fraud check unavailable, breaker open");
                }
            }
        }
        return refuse("fraud check failed twice");
    }

    private Decision refuse(String reason) {
        refusals.merge(reason, 1, Integer::sum);
        if (refusals.values().stream().mapToInt(Integer::intValue).sum() >= alertAfterRefusals) {
            alerted = true;
        }
        return Decision.refuse(reason, "-");
    }

    Map<String, Integer> refusals() {
        return new TreeMap<>(refusals);
    }

    boolean alerted() {
        return alerted;
    }
}
