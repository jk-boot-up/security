package com.jk.explore.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class LimitsTest {

    @Test
    void aBodyAtTheLimitIsReadAndOneByteMoreIsRefused() throws Exception {
        assertEquals(Limits.MAX_BODY_BYTES, Limits.body(new ByteArrayInputStream(new byte[Limits.MAX_BODY_BYTES])).length);
        assertThrows(Rejected.class, () -> Limits.body(new ByteArrayInputStream(new byte[Limits.MAX_BODY_BYTES + 1])));
    }

    @Test
    void cartsHaveAMaximumNumberOfLines() throws Rejected {
        assertEquals(50, Limits.cart(Collections.nCopies(50, "x")).size());
        assertThrows(Rejected.class, () -> Limits.cart(Collections.nCopies(51, "x")));
    }

    @Test
    void theNestedPatternAgreesWithTheLinearCheck() {
        Backtracking engine = new Backtracking();
        for (String input : new String[] {"a", "aaaa", "aaab", "", "b", "aaaaaaa!"}) {
            assertEquals(engine.linear(input), engine.nested(input), input);
        }
    }

    @Test
    void theNestedPatternDoublesItsWorkPerCharacterAndTheLinearOneDoesNot() {
        Backtracking engine = new Backtracking();
        engine.nested("a".repeat(10) + "!");
        long ten = engine.steps();
        engine.nested("a".repeat(11) + "!");
        long eleven = engine.steps();
        assertTrue(eleven > 2 * ten - 10);
        engine.linear("a".repeat(11) + "!");
        assertEquals(12, engine.steps());
    }

    @Test
    void aMatchingInputIsFoundQuickly() {
        Backtracking engine = new Backtracking();
        assertTrue(engine.nested("a".repeat(30)));
        assertFalse(engine.steps() > 30);
    }
}
