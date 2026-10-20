package com.jk.explore.security.fuzz;

import java.util.List;

/** Runs the order file fuzzing story in four numbered acts and prints exact, repeatable results. The random seed is fixed, so every run finds the same things. */
public class FuzzDemo {

    static final long SEED = 9;
    static final int ITERATIONS = 5_000;
    static final List<String> SEEDS = List.of(
            "ORDER v2\nKTL-001;3;24.50\n",
            "ORDER v2\n# weekly order\n@discount 10%\nFLR-002;5kg;1.20\nPAN-003;1;42.00\n");

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Fuzzer fuzzer = new Fuzzer(new BuggyOrderParser(), SEED);

        out.append("== 1. The parser works on the files it was written for ==\n");
        for (String s : SEEDS) {
            out.append(show(s)).append(" -> ").append(parseOutcome(new BuggyOrderParser(), s)).append('\n');
        }

        out.append("\n== 2. Three ways to make inputs, ").append(ITERATIONS).append(" runs each ==\n");
        Fuzzer.Result random = fuzzer.run(SEEDS, ITERATIONS, Strategy.RANDOM_TEXT);
        Fuzzer.Result mutated = fuzzer.run(SEEDS, ITERATIONS, Strategy.MUTATE_SEEDS);
        Fuzzer.Result guided = fuzzer.run(SEEDS, ITERATIONS, Strategy.COVERAGE_GUIDED);
        for (Fuzzer.Result r : List.of(random, mutated, guided)) {
            out.append(String.format("%-16s branches covered: %2d of 16, bugs found: %d, corpus: %d%n", r.strategy(), r.branchesCovered(), r.crashes().size(), r.corpusSize()));
        }

        out.append("\n== 3. Each bug, as the smallest input that still triggers it ==\n");
        for (Crash c : guided.crashes()) {
            out.append(String.format("%-48s run %5d: %s%n", c.signature(), c.foundAtExecution(), show(c.minimized())));
        }

        out.append("\n== 4. After the fix ==\n");
        for (Crash c : guided.crashes()) {
            out.append(String.format("%-48s -> %s%n", show(c.minimized()), parseOutcome(new FixedOrderParser(), c.minimized())));
        }
        Fuzzer.Result after = new Fuzzer(new FixedOrderParser(), SEED).run(SEEDS, ITERATIONS, Strategy.COVERAGE_GUIDED);
        out.append("fuzzing the fixed parser the same way: ").append(after.crashes().size()).append(" bugs in ").append(ITERATIONS).append(" runs (which proves little: it is a search, not a proof)\n");
        return out.toString();
    }

    static String parseOutcome(OrderParser p, String text) {
        try {
            return p.parse(text, new Coverage()).size() + " line(s) parsed";
        } catch (ParseException e) {
            return "refused: " + e.getMessage();
        } catch (RuntimeException e) {
            return "CRASH " + e.getClass().getSimpleName();
        }
    }

    static String show(String s) {
        return "\"" + s.replace("\n", "\\n") + "\"";
    }
}
