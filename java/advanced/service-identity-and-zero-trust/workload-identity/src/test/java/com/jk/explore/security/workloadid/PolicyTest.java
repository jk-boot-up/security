package com.jk.explore.security.workloadid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyTest {

    @Test
    void paymentsMayTransferAndReportingMayReadStatements() {
        assertTrue(Policy.allows("spiffe://bank.example/payments", "POST", "/transfers"));
        assertTrue(Policy.allows("spiffe://bank.example/reporting", "GET", "/statements"));
    }

    @Test
    void anIdentityOnlyGetsItsOwnRoutes() {
        assertFalse(Policy.allows("spiffe://bank.example/payments", "GET", "/statements"));
        assertFalse(Policy.allows("spiffe://bank.example/reporting", "POST", "/transfers"));
    }

    @Test
    void validIdentityWithoutARuleGetsNothing() {
        assertFalse(Policy.allows("spiffe://bank.example/fraud-engine", "POST", "/transfers"));
    }

    @Test
    void anotherTrustDomainOrNoIdentityGetsNothing() {
        assertFalse(Policy.allows("spiffe://other.example/payments", "POST", "/transfers"));
        assertFalse(Policy.allows("none", "POST", "/transfers"));
        assertFalse(Policy.allows(null, "POST", "/transfers"));
    }
}
