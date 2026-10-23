package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoscaTest {

    @Test
    void exposedExactlyWhenSecrecyPlusMigrationExceedsTheThreat() {
        Mosca.DataClass d = new Mosca.DataClass("x", 10, 3);
        assertEquals(13, d.needed());
        assertTrue(d.exposed(12));
        assertFalse(d.exposed(13));
        assertFalse(d.exposed(14));
    }

    @Test
    void startWithinIsNegativeWhenLate() {
        Mosca.DataClass d = new Mosca.DataClass("x", 10, 3);
        assertEquals(2, d.startWithin(15));
        assertEquals(-5, d.startWithin(8));
    }
}
