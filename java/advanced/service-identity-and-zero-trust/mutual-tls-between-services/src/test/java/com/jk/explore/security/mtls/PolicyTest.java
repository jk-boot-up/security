package com.jk.explore.security.mtls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyTest {

    private final Policy policy = Policy.ledger();

    @Test
    void paymentsMovesMoneyAndReportingReadsStatements() {
        assertTrue(policy.allows("payments", "POST", "/transfers"));
        assertTrue(policy.allows("reporting", "GET", "/statements"));
    }

    @Test
    void theOtherWayRoundIsRefused() {
        assertFalse(policy.allows("reporting", "POST", "/transfers"));
        assertFalse(policy.allows("payments", "GET", "/statements"));
    }

    @Test
    void anIdentityWithNoRuleGetsNothing() {
        assertFalse(policy.allows("fraud-engine", "POST", "/transfers"));
        assertFalse(policy.allows("", "POST", "/transfers"));
    }

    @Test
    void anUnknownEndpointIsNotKnown() {
        assertFalse(policy.knows("DELETE", "/transfers"));
        assertFalse(policy.allows("payments", "DELETE", "/transfers"));
    }
}
