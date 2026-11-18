package com.jk.explore.security.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

/** The traces the demo prints: how an input is made, which inputs guidance keeps, and how a crash shrinks. */
class TraceTest {

    @Test
    void namingAMutationDoesNotChangeIt() {
        var one = RandomGeneratorFactory.of(Fuzzer.ALGORITHM).create(7);
        var two = RandomGeneratorFactory.of(Fuzzer.ALGORITHM).create(7);
        for (int i = 0; i < 1000; i++) {
            Fuzzer.Mutated named = Fuzzer.mutateAndName("SHO-1042,2,8990", one);
            assertEquals(Fuzzer.mutate("SHO-1042,2,8990", two), named.text());
            assertTrue(named.how().matches("(flip|insert|delete|replace|number|nest)( \\+ (flip|insert|delete|replace|number|nest))?"));
        }
    }

    @Test
    void theFirstRunsAreRecordedInOrder() {
        Fuzzer.Report report = Fuzzer.run(Harness.CARELESS_ORDERS, FuzzingDemo.ORDER_SEEDS, 100, FuzzingDemo.SEED, false);
        assertEquals(Fuzzer.FIRST_RUNS, report.firstRuns().size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), report.firstRuns().stream().map(Fuzzer.Step::run).toList());
        assertTrue(FuzzingDemo.ORDER_SEEDS.contains(report.firstRuns().getFirst().base()));
    }

    @Test
    void guidanceKeepsTheFieldNameOneLetterAtATime() {
        Fuzzer.Report guided = Fuzzer.run(Harness.CARELESS_CARTS, FuzzingDemo.CART_SEEDS, FuzzingDemo.CART_BUDGET, FuzzingDemo.SEED, true);
        List<Integer> letters = new ArrayList<>();
        for (Fuzzer.Kept kept : guided.kept()) {
            kept.newBranches().stream().filter(b -> b >= 50 && b <= 53).forEach(letters::add);
        }
        assertEquals(List.of(50, 51, 52, 53), letters);
        assertEquals(guided.corpus(), FuzzingDemo.CART_SEEDS.size() + guided.kept().size());
        assertTrue(guided.kept().getLast().run() < guided.firstRun(FuzzingDemo.GIFT_BUG));
    }

    @Test
    void everyShrinkingStepStillCrashesTheSameWay() {
        List<String> steps = new ArrayList<>();
        String smallest = Minimizer.minimise(Harness.CARELESS_CARTS, "[{\"note\":\"#AG-0007\",\"qty\"1}]", steps);
        assertEquals(smallest, steps.getLast());
        for (String step : steps) {
            assertEquals(FuzzingDemo.GIFT_BUG, Crash.signature(Fuzzer.execute(Harness.CARELESS_CARTS, step).failure()));
        }
    }

    @Test
    void theOverflowIsTheLow32BitsOfTheExactTotal() {
        long exact = 999_999_999L * 4500;
        assertEquals(4_499_999_995_500L, exact);
        assertEquals(-1_125_730_708, (int) exact);
        assertEquals(3_169_236_588L - (1L << 32), (int) exact);
    }

    @Test
    void controlCharactersArePrintedSoTheyCanBeSeen() {
        assertEquals("450\\x10", FuzzingDemo.visible("450\u0010"));
        assertEquals("\"SHO-10...\" 40 chars", FuzzingDemo.brief("SHO-1042" + "x".repeat(32)));
    }
}
