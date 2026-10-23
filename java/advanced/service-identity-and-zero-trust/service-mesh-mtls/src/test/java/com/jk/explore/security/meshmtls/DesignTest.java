package com.jk.explore.security.meshmtls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DesignTest {

    private static final Set<String> DESIGN = Set.of(Design.pair(Lab.WEB, Lab.PAYMENTS), Design.pair(Lab.PAYMENTS, Lab.LEDGER));

    @Test
    void fiveCallersAndFourServicesMakeSixteenPairs() {
        assertEquals(16, Lab.DESIGN.compare(Lab.CALLERS, Lab.BANK, Set.of()).total());
    }

    @Test
    void aMeasurementEqualToTheDesignMatches() {
        Design.Diff d = Lab.DESIGN.compare(Lab.CALLERS, Lab.BANK, DESIGN);
        assertTrue(d.matches());
        assertEquals(2, d.answered());
    }

    @Test
    void anAnsweredPairThatTheDesignDoesNotListIsUnwanted() {
        Set<String> measured = new HashSet<>(DESIGN);
        measured.add(Design.pair(Lab.STATEMENTS, Lab.LEDGER));
        Design.Diff d = Lab.DESIGN.compare(Lab.CALLERS, Lab.BANK, measured);
        assertEquals(List.of("bank/statements -> bank/ledger"), d.unwanted());
        assertFalse(d.matches());
    }

    @Test
    void aRefusedPairThatTheDesignListsIsMissing() {
        Design.Diff d = Lab.DESIGN.compare(Lab.CALLERS, Lab.BANK, Set.of(Design.pair(Lab.PAYMENTS, Lab.LEDGER)));
        assertEquals(List.of("bank/web -> bank/payments"), d.missing());
    }
}
