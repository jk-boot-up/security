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

    private static final String[] BOUNDARIES = {"0", "1", "99", "100", "999999999", "2147483647", "-1"};

    record Report(String harness, int runs, Map<String, Crash> crashes, int branches, int corpus, List<String> parsed) {

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
        for (int runNumber = 1; runNumber <= budget; runNumber++) {
            String base;
            if (!guided) {
                base = seeds.get(random.nextInt(seeds.size()));
            } else if (random.nextBoolean()) {
                base = corpus.getLast();
            } else {
                base = corpus.get(random.nextInt(corpus.size()));
            }
            String input = mutate(base, random);
            Outcome outcome = execute(harness, input);
            if (outcome.failure() != null) {
                crashes.putIfAbsent(Crash.signature(outcome.failure()), new Crash(Crash.signature(outcome.failure()), runNumber, input));
                continue;
            }
            if (outcome.parsed() && parsed.size() < 2000) {
                parsed.add(input);
            }
            if (outcome.coverage().addsTo(seen)) {
                seen.or(outcome.coverage().hits());
                if (guided) {
                    corpus.add(input);
                }
            }
        }
        return new Report(harness.name(), budget, crashes, seen.cardinality(), corpus.size(), parsed);
    }

    record Outcome(Coverage coverage, Throwable failure, boolean parsed) {
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

    static String mutate(String input, RandomGenerator random) {
        String out = input;
        int changes = 1 + random.nextInt(2);
        for (int i = 0; i < changes; i++) {
            out = mutateOnce(out, random);
        }
        return out;
    }

    private static String mutateOnce(String input, RandomGenerator random) {
        StringBuilder text = new StringBuilder(input);
        int kind = random.nextInt(100);
        int at = text.isEmpty() ? 0 : random.nextInt(text.length());
        if (kind < 25 && !text.isEmpty()) {
            char c = text.charAt(at);
            text.setCharAt(at, (char) ((c ^ (1 << random.nextInt(7))) & 0x7f));
        } else if (kind < 45) {
            text.insert(at, printable(random));
        } else if (kind < 65 && !text.isEmpty()) {
            text.deleteCharAt(at);
        } else if (kind < 85 && !text.isEmpty()) {
            text.setCharAt(at, printable(random));
        } else if (kind < 99) {
            return replaceNumber(input, random);
        } else {
            text.insert(at, "[".repeat(DEEP_NESTING));
        }
        return text.toString();
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
