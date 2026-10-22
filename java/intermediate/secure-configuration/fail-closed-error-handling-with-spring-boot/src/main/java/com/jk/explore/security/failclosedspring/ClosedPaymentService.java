package com.jk.explore.security.failclosedspring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * The decision starts as no. Only a fraud check that ran, answered in time and said low risk turns it into yes. Every other path ends in an exception
 * that the error handler turns into a refusal. The fraud call has a time budget (in the client), one retry (Spring Framework 7's {@link RetryTemplate})
 * and a breaker, and each failure is counted by reason.
 */
@Service
@ConditionalOnProperty(name = "bank.mode", havingValue = "closed")
class ClosedPaymentService implements PaymentService {

    private final FraudClient fraud;
    private final RetryTemplate retry;
    private final Breaker breaker;
    private final MeterRegistry meters;

    ClosedPaymentService(FraudClient fraud, RetryTemplate retry, Breaker breaker, MeterRegistry meters) {
        this.fraud = fraud;
        this.retry = retry;
        this.breaker = breaker;
        this.meters = meters;
    }

    @Override
    public Decision decide(Payment payment) {
        boolean risky;
        try {
            risky = breaker.call(() -> checked(payment));
        } catch (CheckFailedException e) {
            meters.counter("payments.refused", "reason", e.why).increment();
            throw e;
        }
        if (risky) {
            meters.counter("payments.refused", "reason", "looks fraudulent").increment();
            return new Decision(false, "looks fraudulent");
        }
        return new Decision(true, "approved");
    }

    private boolean checked(Payment payment) {
        try {
            return retry.execute(() -> fraud.looksFraudulent(payment));
        } catch (RetryException e) {
            String why = e.getLastException() instanceof FraudClient.CallFailedException f && f.slow
                    ? "fraud check too slow" : "fraud check unavailable";
            throw new CheckFailedException(why, "fraud check failed for card " + payment.card() + " at " + payment.merchant() + ": " + e.getLastException(), e);
        }
    }
}
