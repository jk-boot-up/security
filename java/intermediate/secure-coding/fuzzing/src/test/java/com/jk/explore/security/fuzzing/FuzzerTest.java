package com.jk.explore.security.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class FuzzerTest {

    @Test
    void theSameSeedFindsTheSameCrashesAtTheSameRuns() {
        Fuzzer.Report a = Fuzzer.run(Harness.CARELESS_ORDERS, FuzzingDemo.ORDER_SEEDS, 5_000, 3, false);
        Fuzzer.Report b = Fuzzer.run(Harness.CARELESS_ORDERS, FuzzingDemo.ORDER_SEEDS, 5_000, 3, false);
        assertEquals(a.crashes(), b.crashes());
        assertFalse(a.crashes().isEmpty());
    }

    @Test
    void mutationIsRepeatable() {
        var one = RandomGeneratorFactory.of(Fuzzer.ALGORITHM).create(11);
        var two = RandomGeneratorFactory.of(Fuzzer.ALGORITHM).create(11);
        for (int i = 0; i < 1000; i++) {
            assertEquals(Fuzzer.mutate("SHO-1042,2,8990", one), Fuzzer.mutate("SHO-1042,2,8990", two));
        }
    }

    @Test
    void blindFuzzingFindsTheShallowOrderBugs() {
        Fuzzer.Report report = Fuzzer.run(Harness.CARELESS_ORDERS, FuzzingDemo.ORDER_SEEDS, FuzzingDemo.ORDER_BUDGET, FuzzingDemo.SEED, false);
        assertTrue(report.crashes().containsKey("ArrayIndexOutOfBoundsException in OrderLine.parseCareless"));
        assertTrue(report.crashes().containsKey("IllegalArgumentException in Money.<init>"));
    }

    @Test
    void onlyCoverageGuidanceReachesTheGiftMessageBug() {
        Fuzzer.Report blind = Fuzzer.run(Harness.CARELESS_CARTS, FuzzingDemo.CART_SEEDS, FuzzingDemo.CART_BUDGET, FuzzingDemo.SEED, false);
        Fuzzer.Report guided = Fuzzer.run(Harness.CARELESS_CARTS, FuzzingDemo.CART_SEEDS, FuzzingDemo.CART_BUDGET, FuzzingDemo.SEED, true);
        assertEquals(-1, blind.firstRun(FuzzingDemo.GIFT_BUG));
        assertTrue(guided.firstRun(FuzzingDemo.GIFT_BUG) > 0);
        assertTrue(guided.branches() > blind.branches());
        assertTrue(blind.firstRun(FuzzingDemo.NESTING_BUG) > 0);
    }

    @Test
    void theFixedParsersSurviveTheFuzzer() {
        assertTrue(Fuzzer.run(Harness.ORDERS, FuzzingDemo.ORDER_SEEDS, 10_000, 5, true).crashes().isEmpty());
        assertTrue(Fuzzer.run(Harness.CARTS, FuzzingDemo.CART_SEEDS, 20_000, 5, true).crashes().isEmpty());
    }

    @Test
    void aParseExceptionIsNotACrash() {
        assertEquals(null, Fuzzer.execute(Harness.CARTS, "{").failure());
        assertFalse(Fuzzer.execute(Harness.CARTS, "{").parsed());
    }

    @Test
    void crashesAreNamedByTypeAndPlace() {
        Throwable failure = Fuzzer.execute(Harness.CARELESS_ORDERS, "SHO-1042").failure();
        assertEquals("ArrayIndexOutOfBoundsException in OrderLine.parseCareless", Crash.signature(failure));
        assertEquals("\"SHO-1042\"", Crash.show("SHO-1042"));
        assertEquals("50 characters starting \"aaaaaaaaaaaa\"", Crash.show("a".repeat(50)));
    }
}
