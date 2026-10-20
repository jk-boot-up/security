package com.jk.explore.security.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinimizerTest {

    @Test
    void aCrashIsReducedToItsSmallestForm() {
        assertEquals("SHO-1042", Minimizer.minimise(Harness.CARELESS_ORDERS, "SHO-1042,28990"));
        // the careless parser crashes before it reaches the closing brace, so the brace is not needed
        assertEquals("{\"note\":\"#\"", Minimizer.minimise(Harness.CARELESS_CARTS, "[{\"note\":\"#AG-0007\",\"qty\"1}]"));
    }

    @Test
    void theSmallestFormStillCrashesTheSameWay() {
        String smallest = Minimizer.minimise(Harness.CARELESS_ORDERS, "BAG-0007,999999999,4500");
        Throwable failure = Fuzzer.execute(Harness.CARELESS_ORDERS, smallest).failure();
        assertEquals("IllegalArgumentException in Money.<init>", Crash.signature(failure));
        assertTrue(smallest.length() < "BAG-0007,999999999,4500".length());
    }

    @Test
    void anInputThatDoesNotCrashIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Minimizer.minimise(Harness.CARELESS_ORDERS, "SHO-1042,2,8990"));
    }
}
