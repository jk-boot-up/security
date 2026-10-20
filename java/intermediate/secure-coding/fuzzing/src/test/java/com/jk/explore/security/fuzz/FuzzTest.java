package com.jk.explore.security.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FuzzTest {

    private static final String OK = "ORDER v2\nKTL-001;3;24.50\n";

    private static void assertCleanOutcome(OrderParser parser, String input) {
        try {
            parser.parse(input, new Coverage());
        } catch (ParseException expected) {
            // the only allowed failure
        }
    }

    @Test
    void theBuggyParserReadsGoodFilesAndTheFixedOneReadsTheSame() throws ParseException {
        assertEquals(new OrderLine("KTL-001", 3, 2450, 7350), new BuggyOrderParser().parse(OK, new Coverage()).get(0));
        assertEquals(new BuggyOrderParser().parse(OK, new Coverage()), new FixedOrderParser().parse(OK, new Coverage()));
        for (String seed : FuzzDemo.SEEDS) {
            assertEquals(new BuggyOrderParser().parse(seed, new Coverage()), new FixedOrderParser().parse(seed, new Coverage()));
        }
    }

    @Test
    void eachKnownBugIsReachableByHand() {
        OrderParser p = new BuggyOrderParser();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> p.parse("ORDER v2\nKTL-001;3;24.\n", new Coverage()));
        assertThrows(StringIndexOutOfBoundsException.class, () -> p.parse("ORDER v2\nKTL-001;5g;1.20\n", new Coverage()));
        assertThrows(NumberFormatException.class, () -> p.parse("ORDER v2\n@discount ten%\n", new Coverage()));
        assertThrows(Hang.class, () -> p.parse("ORDER v2\n@repeat 3\n", new Coverage()));
        assertThrows(IllegalStateException.class, () -> p.parse("ORDER v2\nKTL-001;3;-4.00\n", new Coverage()));
    }

    @Test
    void theFixedParserRefusesEachOfThemCleanly() {
        OrderParser p = new FixedOrderParser();
        for (String bad : List.of("ORDER v2\nKTL-001;3;24.\n", "ORDER v2\nKTL-001;5g;1.20\n", "ORDER v2\n@discount ten%\n", "ORDER v2\nKTL-001;999999999;99.99\n",
                "ORDER v2\nKTL-001;3;-4.00\n", "ORDER v2\n@discount 101%\n", "ORDER v2\n@nothing\n", "", "ORDER v2\n" + "x\n".repeat(2000))) {
            assertThrows(ParseException.class, () -> p.parse(bad, new Coverage()), bad);
        }
        assertDoesNotHang();
    }

    private static void assertDoesNotHang() {
        assertCleanOutcome(new FixedOrderParser(), "ORDER v2\n@repeat 3\n");
    }

    @Test
    void theLargestHonestOrderDoesNotOverflow() throws ParseException {
        OrderLine line = new FixedOrderParser().parse("ORDER v2\nKTL-001;999999;999999.99\n", new Coverage()).get(0);
        assertEquals(Integer.MAX_VALUE, line.totalCents(), "the total is capped, not wrapped");
    }

    @Test
    void aFuzzRunIsRepeatableBecauseTheSeedIsFixed() {
        Fuzzer.Result a = new Fuzzer(new BuggyOrderParser(), 5).run(FuzzDemo.SEEDS, 1500, Strategy.COVERAGE_GUIDED);
        Fuzzer.Result b = new Fuzzer(new BuggyOrderParser(), 5).run(FuzzDemo.SEEDS, 1500, Strategy.COVERAGE_GUIDED);
        assertEquals(a, b);
    }

    @Test
    void randomTextNeverGetsPastTheHeaderButSeedsAndFeedbackDo() {
        Fuzzer f = new Fuzzer(new BuggyOrderParser(), FuzzDemo.SEED);
        Fuzzer.Result random = f.run(FuzzDemo.SEEDS, FuzzDemo.ITERATIONS, Strategy.RANDOM_TEXT);
        Fuzzer.Result seeds = f.run(FuzzDemo.SEEDS, FuzzDemo.ITERATIONS, Strategy.MUTATE_SEEDS);
        Fuzzer.Result guided = f.run(FuzzDemo.SEEDS, FuzzDemo.ITERATIONS, Strategy.COVERAGE_GUIDED);
        assertEquals(0, random.crashes().size());
        assertTrue(seeds.crashes().size() >= 5);
        assertTrue(guided.branchesCovered() >= seeds.branchesCovered());
        assertTrue(guided.corpusSize() > FuzzDemo.SEEDS.size(), "inputs that reach new branches are kept");
        Set<String> kinds = guided.crashes().stream().map(c -> c.signature().split(" ")[0]).collect(Collectors.toSet());
        assertTrue(kinds.containsAll(Set.of("ArrayIndexOutOfBoundsException", "StringIndexOutOfBoundsException", "NumberFormatException", "IllegalStateException")));
    }

    @Test
    void everyFindingIsFiledOnceAndItsMinimizedInputStillFailsTheSameWay() {
        Fuzzer f = new Fuzzer(new BuggyOrderParser(), FuzzDemo.SEED);
        Fuzzer.Result r = f.run(FuzzDemo.SEEDS, FuzzDemo.ITERATIONS, Strategy.COVERAGE_GUIDED);
        assertEquals(r.crashes().size(), r.crashes().stream().map(Crash::signature).distinct().count());
        for (Crash c : r.crashes()) {
            assertTrue(c.minimized().length() <= c.input().length());
            assertEquals(c.signature(), f.execute(c.minimized(), new Coverage()), c.signature());
            assertEquals(c.signature(), f.execute(c.input(), new Coverage()));
        }
    }

    @Test
    void aMinimizedInputCannotLoseAnyChunkAndKeepFailing() {
        Fuzzer f = new Fuzzer(new BuggyOrderParser(), 1);
        String min = f.minimize("ORDER v2\n# a long comment that is not needed\nKTL-001;3;24.\nPAN-003;1;42.00\n", "ArrayIndexOutOfBoundsException after branch 14");
        assertTrue(min.length() <= 14, min);
        assertEquals("ArrayIndexOutOfBoundsException after branch 14", f.execute(min, new Coverage()));
        for (int i = 0; i < min.length(); i++) {
            String smaller = min.substring(0, i) + min.substring(i + 1);
            assertFalse("ArrayIndexOutOfBoundsException after branch 14".equals(f.execute(smaller, new Coverage())), "removing character " + i + " still fails: " + smaller);
        }
    }

    @Test
    void theFixedParserSurvivesTheSameFuzzingWithSeveralSeeds() {
        for (long seed : new long[] {1, 2, 3, FuzzDemo.SEED}) {
            Fuzzer.Result r = new Fuzzer(new FixedOrderParser(), seed).run(FuzzDemo.SEEDS, 3000, Strategy.COVERAGE_GUIDED);
            assertEquals(0, r.crashes().size(), "seed " + seed + ": " + r.crashes());
        }
    }

    @Test
    void regressionTestsAreGeneratedFromCrashes() {
        Crash c = new Crash("NumberFormatException after branch 6", "ORDER v2\n@discount \"x\\\n", "ORDER v2\n@discount \"x\\\n", 12);
        String src = Regression.asJavaTest(c, 3);
        assertTrue(src.contains("void fuzzFinding3()"));
        assertTrue(src.contains("found by the fuzzer on run 12"));
        assertTrue(src.contains("\"ORDER v2\\n@discount \\\"x\\\\\\n\""));
    }

    @Test
    void theStepBudgetStopsALoop() {
        Coverage c = new Coverage();
        assertThrows(Hang.class, () -> {
            while (true) {
                c.hit(1);
            }
        });
    }
}
