package com.jk.explore.security.failclosed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthorizerTest {

    private static final Payment COFFEE = new Payment("P-1", "4111111111111111", "corner-cafe", 450);
    private static final Payment RISKY = new Payment("P-2", "4111111111111111", "unknown-electronics", 150_000);

    private SafeAuthorizer safe(SimClock clock, FraudService fraud, SecurityLog log) {
        return new SafeAuthorizer(fraud, clock, 200_000, new References(), log);
    }

    @Test
    void theNaiveAuthorizerApprovesWhenTheFraudCheckIsDown() {
        FraudService fraud = new FraudService(new SimClock(), FraudService.Mode.BROKEN);
        assertTrue(new NaiveAuthorizer(fraud, 200_000).authorize(RISKY, "0").approved());
    }

    @Test
    void theNaiveAuthorizerSkipsTheLimitWhenTheRecordIsUnreadable() {
        FraudService fraud = new FraudService(new SimClock(), FraudService.Mode.HEALTHY);
        Payment big = new Payment("P-3", "4111111111111111", "shop", 450_000);
        assertFalse(new NaiveAuthorizer(fraud, 200_000).authorize(big, "0").approved());
        assertTrue(new NaiveAuthorizer(fraud, 200_000).authorize(big, "n/a").approved());
    }

    @Test
    void theSafeAuthorizerApprovesOnlyWhenEveryCheckRanAndPassed() {
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.HEALTHY);
        SafeAuthorizer safe = safe(clock, fraud, new SecurityLog());
        assertTrue(safe.authorize(COFFEE, "0").approved());
        assertEquals("looks fraudulent", safe.authorize(RISKY, "0").reason());
        assertEquals("over the daily limit", safe.authorize(COFFEE, "199900").reason());
    }

    @Test
    void errorsTimeoutsAndBadValuesAreRefusals() {
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.BROKEN);
        SecurityLog log = new SecurityLog();
        SafeAuthorizer safe = safe(clock, fraud, log);
        assertFalse(safe.authorize(COFFEE, "0").approved());
        fraud.setMode(FraudService.Mode.SLOW);
        assertFalse(safe.authorize(COFFEE, "0").approved());
        fraud.setMode(FraudService.Mode.HEALTHY);
        assertFalse(safe.authorize(COFFEE, "n/a").approved());
        assertFalse(safe.authorize(COFFEE, null).approved());
        assertFalse(safe.authorize(COFFEE, "-5").approved());
        assertEquals(5, log.lines().size());
    }

    @Test
    void everyDecisionHasItsOwnReference() {
        SimClock clock = new SimClock();
        SafeAuthorizer safe = safe(clock, new FraudService(clock, FraudService.Mode.HEALTHY), new SecurityLog());
        assertEquals("REF-0001", safe.authorize(COFFEE, "0").reference());
        assertEquals("REF-0002", safe.authorize(COFFEE, "0").reference());
    }
}
