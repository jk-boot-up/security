package com.jk.explore.security.k8snetpol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class IntentTest {

    private static final Set<String> DESIGN = Set.of(Intent.pair(Lab.WEB, Lab.API), Intent.pair(Lab.API, Lab.DB), Intent.pair(Lab.ADMIN, Lab.DB));

    @Test
    void fivePodsMakeTwentyPairs() {
        assertEquals(20, Lab.INTENT.compare(Lab.PODS, Set.of()).total());
    }

    @Test
    void aMeasurementEqualToTheDesignMatches() {
        Intent.Diff d = Lab.INTENT.compare(Lab.PODS, DESIGN);
        assertTrue(d.matches());
        assertEquals(3, d.reachable());
    }

    @Test
    void anOpenPairThatTheDesignDoesNotListIsUnwanted() {
        Set<String> measured = new java.util.HashSet<>(DESIGN);
        measured.add(Intent.pair(Lab.JOBS, Lab.API));
        Intent.Diff d = Lab.INTENT.compare(Lab.PODS, measured);
        assertEquals(java.util.List.of("shop/jobs -> pay/api"), d.unwanted());
        assertFalse(d.matches());
    }

    @Test
    void aClosedPairThatTheDesignListsIsMissing() {
        Intent.Diff d = Lab.INTENT.compare(Lab.PODS, Set.of(Intent.pair(Lab.API, Lab.DB), Intent.pair(Lab.ADMIN, Lab.DB)));
        assertEquals(java.util.List.of("shop/web -> pay/api"), d.missing());
    }

    @Test
    void nothingOpenAtAllIsThreeMissing() {
        assertEquals(3, Lab.INTENT.compare(Lab.PODS, Set.of()).missing().size());
    }
}
