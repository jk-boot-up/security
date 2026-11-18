package com.jk.explore.security.fuzzing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * A small mutation fuzzer. It starts from seed inputs, changes them a little, runs the parser, and records every failure that is
 * not a {@link ParseException}. Blind, it always mutates a seed. Guided by coverage, it keeps every input that reached a branch no
 * earlier input reached, and mutates those too (the newest one half the time), so it can creep one branch at a time towards
 * code deep inside the parser.
 * The random numbers come from a named algorithm with a fixed seed, so every run finds the same crashes in the same order.
 */
final class Fuzzer {

    static final String ALGORITHM = "L64X128MixRandom";
    static final int DEEP_NESTING = 200_000;
    static final int FIRST_RUNS = 6;

    static final String[] BOUNDARIES = {"0", "1", "99", "100", "999999999", "2147483647", "-1"};

    /** The first runs of a fuzzing session, kept to show how one input is made: the base, the change and what happened. */
    record Step(int run, String base, String how, String input, String result) {
    }

    /** An input the guided fuzzer kept, because it reached branches no earlier input reached. */
    record Kept(int run, String input, List<Integer> newBranches) {
    }

    record Report(String harness, int runs, Map<String, Crash> crashes, int branches, int corpus, List<String> parsed,
                  List<Step> firstRuns, List<Kept> kept) {

        /** The run that first found a crash of this signature, or -1. */
        int firstRun(String signature) {
            Crash crash = crashes.get(signature);
            return crash == null ? -1 : crash.run();
        }
    }

    private Fuzzer() {
    }

    static Report run(Harness harness, List<String> seeds, int budget, long seed, boolean guided) {
        RandomGenerator random = RandomGeneratorFactory.of(ALGORITHM).create(seed);
        List<String> corpus = new ArrayList<>(seeds);
        BitSet seen = new BitSet();
        for (String s : seeds) {
            seen.or(execute(harness, s).coverage().hits());
        }
        Map<String, Crash> crashes = new LinkedHashMap<>();
        List<String> parsed = new ArrayList<>();
        List<Step> firstRuns = new ArrayList<>();
        List<Kept> kept = new ArrayList<>();
        for (int runNumber = 1; runNumber <= budget; runNumber++) {
            String base;
            if (!guided) {
                base = seeds.get(random.nextInt(seeds.size()));
            } else if (random.nextBoolean()) {
                base = corpus.getLast();
            } else {
                base = corpus.get(random.nextInt(corpus.size()));
            }
            Mutated mutated = mutateAndName(base, random);
            String input = mutated.text();
            Outcome outcome = execute(harness, input);
            if (firstRuns.size() < FIRST_RUNS) {
                firstRuns.add(new Step(runNumber, base, mutated.how(), input, outcome.describe()));
            }
            if (outcome.failure() != null) {
                crashes.putIfAbsent(Crash.signature(outcome.failure()), new Crash(Crash.signature(outcome.failure()), runNumber, input));
                continue;
            }
            if (outcome.parsed() && parsed.size() < 2000) {
                parsed.add(input);
            }
            if (outcome.coverage().addsTo(seen)) {
                List<Integer> fresh = outcome.coverage().newBranches(seen);
                seen.or(outcome.coverage().hits());
                if (guided) {
                    corpus.add(input);
                    kept.add(new Kept(runNumber, input, fresh));
                }
            }
        }
        return new Report(harness.name(), budget, crashes, seen.cardinality(), corpus.size(), parsed, firstRuns, kept);
    }

    record Outcome(Coverage coverage, Throwable failure, boolean parsed) {

        /** "parsed", "refused" or "crash", as the demo prints it. */
        String describe() {
            return failure != null ? "crash" : parsed ? "parsed" : "refused";
        }
    }

    static Outcome execute(Harness harness, String input) {
        Coverage coverage = new Coverage();
        try {
            harness.run(input, coverage);
            return new Outcome(coverage, null, true);
        } catch (ParseException expected) {
            return new Outcome(coverage, null, false);
        } catch (RuntimeException | StackOverflowError bug) {
            return new Outcome(coverage, bug, false);
        }
    }

    /** A mutated input, and the names of the changes that made it, such as "delete + insert". */
    record Mutated(String text, String how) {
    }

    static String mutate(String input, RandomGenerator random) {
        return mutateAndName(input, random).text();
    }

    static Mutated mutateAndName(String input, RandomGenerator random) {
        Mutated out = new Mutated(input, "");
        int changes = 1 + random.nextInt(2);
        for (int i = 0; i < changes; i++) {
            Mutated once = mutateOnce(out.text(), random);
            out = new Mutated(once.text(), out.how().isEmpty() ? once.how() : out.how() + " + " + once.how());
        }
        return out;
    }

    private static Mutated mutateOnce(String input, RandomGenerator random) {
        StringBuilder text = new StringBuilder(input);
        int kind = random.nextInt(100);
        int at = text.isEmpty() ? 0 : random.nextInt(text.length());
        String how;
        if (kind < 25 && !text.isEmpty()) {
            char c = text.charAt(at);
            text.setCharAt(at, (char) ((c ^ (1 << random.nextInt(7))) & 0x7f));
            how = "flip";
        } else if (kind < 45) {
            text.insert(at, printable(random));
            how = "insert";
        } else if (kind < 65 && !text.isEmpty()) {
            text.deleteCharAt(at);
            how = "delete";
        } else if (kind < 85 && !text.isEmpty()) {
            text.setCharAt(at, printable(random));
            how = "replace";
        } else if (kind < 99) {
            return new Mutated(replaceNumber(input, random), "number");
        } else {
            text.insert(at, "[".repeat(DEEP_NESTING));
            how = "nest";
        }
        return new Mutated(text.toString(), how);
    }

    private static char printable(RandomGenerator random) {
        return (char) (32 + random.nextInt(95));
    }

    private static String replaceNumber(String input, RandomGenerator random) {
        List<int[]> runs = new ArrayList<>();
        for (int i = 0; i < input.length(); ) {
            if (Character.isDigit(input.charAt(i))) {
                int start = i;
                while (i < input.length() && Character.isDigit(input.charAt(i))) {
                    i++;
                }
                runs.add(new int[] {start, i});
            } else {
                i++;
            }
        }
        if (runs.isEmpty()) {
            return input;
        }
        int[] run = runs.get(random.nextInt(runs.size()));
        return input.substring(0, run[0]) + BOUNDARIES[random.nextInt(BOUNDARIES.length)] + input.substring(run[1]);
    }
}
