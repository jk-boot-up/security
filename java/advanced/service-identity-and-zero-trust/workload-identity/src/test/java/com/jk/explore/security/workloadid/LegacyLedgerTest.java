package com.jk.explore.security.workloadid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LegacyLedgerTest {

    @Test
    void aWrongOrMissingKeyIsUnauthorized() {
        assertEquals(401, LegacyLedger.decide("s", "x", "payments", "POST", "/transfers"));
        assertEquals(401, LegacyLedger.decide("s", null, "payments", "POST", "/transfers"));
    }

    @Test
    void theKeyHolderIsBelievedWhateverNameItGives() {
        assertEquals(200, LegacyLedger.decide("s", "s", "payments", "POST", "/transfers"));
        assertEquals(403, LegacyLedger.decide("s", "s", "reporting", "POST", "/transfers"));
        // the same key with a different claim opens the other door: the claim is the only thing the ledger checks
        assertEquals(200, LegacyLedger.decide("s", "s", "reporting", "GET", "/statements"));
    }
}
