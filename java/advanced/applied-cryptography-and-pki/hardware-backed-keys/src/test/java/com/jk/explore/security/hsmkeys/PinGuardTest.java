package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PinGuardTest {

    @Test
    void locksAfterTheMaximumAndStopsAskingTheToken() {
        int[] asked = {0};
        PinGuard g = new PinGuard(3, p -> {
            asked[0]++;
            return p.equals("right");
        });
        assertEquals(PinGuard.Outcome.REFUSED, g.attempt("a"));
        assertEquals(PinGuard.Outcome.REFUSED, g.attempt("b"));
        assertEquals(PinGuard.Outcome.REFUSED, g.attempt("c"));
        assertTrue(g.locked());
        assertEquals(PinGuard.Outcome.LOCKED, g.attempt("right"));
        assertEquals(3, asked[0]);
    }

    @Test
    void aRightPinResetsTheCountAndAResetUnlocks() {
        PinGuard g = new PinGuard(2, p -> p.equals("right"));
        g.attempt("x");
        assertEquals(PinGuard.Outcome.ACCEPTED, g.attempt("right"));
        g.attempt("x");
        assertFalse(g.locked());
        g.attempt("x");
        assertTrue(g.locked());
        g.reset();
        assertEquals(PinGuard.Outcome.ACCEPTED, g.attempt("right"));
    }
}
