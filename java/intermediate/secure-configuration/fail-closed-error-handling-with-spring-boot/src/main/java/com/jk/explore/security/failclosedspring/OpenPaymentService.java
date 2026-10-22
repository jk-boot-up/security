package com.jk.explore.security.failclosedspring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** The version from a hurry: the decision starts as yes, and a catch-all keeps the payment flowing when the check breaks. */
@Service
@ConditionalOnProperty(name = "bank.mode", havingValue = "open")
class OpenPaymentService implements PaymentService {

    private final FraudClient fraud;

    OpenPaymentService(FraudClient fraud) {
        this.fraud = fraud;
    }

    @Override
    public Decision decide(Payment payment) {
        try {
            if (fraud.looksFraudulent(payment)) {
                return new Decision(false, "looks fraudulent");
            }
        } catch (Exception e) {
            // the fraud service is having a bad day; do not bother the customer
        }
        return new Decision(true, "approved");
    }
}
