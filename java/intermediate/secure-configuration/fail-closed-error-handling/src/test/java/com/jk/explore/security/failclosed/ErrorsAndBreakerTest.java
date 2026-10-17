package com.jk.explore.security.failclosed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ErrorsAndBreakerTest {

    private static final Payment COFFEE = new Payment("P-1", "4111111111111111", "corner-cafe", 450);

    @Test
    void theSafePageHasNoInternalDetail() {
        String page = ErrorPages.safe("REF-0042");
        assertTrue(page.contains("REF-0042"));
        assertFalse(page.contains("Exception"));
        assertFalse(page.contains("internal"));
    }

    @Test
    void theNaivePageLeaksTheException() {
        String page = ErrorPages.naive(new ServiceException("db.internal down for card 4111111111111111"));
        assertTrue(page.contains("db.internal"));
        assertTrue(page.contains("ServiceException"));
    }

    @Test
    void theLogNeverHoldsAFullCardNumber() {
        assertEquals("card ****1111 refused", SecurityLog.redact("card 4111111111111111 refused"));
        assertEquals("ref 1234 kept", SecurityLog.redact("ref 1234 kept"));
        assertEquals("amex ****0005", SecurityLog.redact("amex 378282246310005"));
    }

    @Test
    void theBreakerOpensAfterFailuresAndClosesAfterASuccessfulTrial() {
        SimClock clock = new SimClock();
        CircuitBreaker breaker = new CircuitBreaker(clock, 3, 1_000);
        breaker.failure();
        breaker.failure();
        assertTrue(breaker.allowsCall());
        breaker.failure();
        assertFalse(breaker.allowsCall());
        clock.advance(1_000);
        assertTrue(breaker.allowsCall());
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state());
        breaker.failure();
        assertFalse(breaker.allowsCall());
        clock.advance(1_000);
        assertTrue(breaker.allowsCall());
        breaker.success();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void anOpenBreakerRefusesWithoutCallingTheService() {
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.BROKEN);
        ResilientAuthorizer authorizer = new ResilientAuthorizer(fraud, clock, new CircuitBreaker(clock, 3, 30_000), 3);
        for (int i = 0; i < 6; i++) {
            assertFalse(authorizer.authorize(COFFEE).approved());
        }
        assertEquals(3, fraud.calls());
        assertTrue(authorizer.alerted());
    }

    @Test
    void aFastFailureIsRetriedOnce() {
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.BROKEN);
        ResilientAuthorizer authorizer = new ResilientAuthorizer(fraud, clock, new CircuitBreaker(clock, 5, 30_000), 100);
        assertFalse(authorizer.authorize(COFFEE).approved());
        assertEquals(2, fraud.calls());
    }
}
