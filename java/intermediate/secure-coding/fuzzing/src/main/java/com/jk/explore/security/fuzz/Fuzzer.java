package com.jk.explore.security.fuzz;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * A small fuzzer. It makes inputs, runs the parser, and files anything that is not a clean parse or a {@link ParseException} as a crash, once for each
 * kind of failure. It uses a fixed random seed, so a run is repeatable, which is also what makes a crash reproducible.
 */
public final class Fuzzer {

    /** What a run found. */
    public record Result(Strategy strategy, int executions, int branchesCovered, int corpusSize, List<Crash> crashes) {
    }

    private static final String[] DICTIONARY = {"ORDER v2\n", "\n", "@discount ", "@repeat ", ";", "kg", "g", "%", ".", "0", "1", "-1", "99999999", "#", "ABC-123"};

    private final OrderParser parser;
    private final long seed;

    public Fuzzer(OrderParser parser, long seed) {
        this.parser = parser;
        this.seed = seed;
    }

    public Result run(List<String> seeds, int iterations, Strategy strategy) {
        SplittableRandom rnd = new SplittableRandom(seed);
        List<String> corpus = new ArrayList<>(seeds);
        BitSet seen = new BitSet();
        Map<String, Crash> crashes = new LinkedHashMap<>();
        for (int n = 1; n <= iterations; n++) {
            String input = switch (strategy) {
                case RANDOM_TEXT -> randomText(rnd);
                case MUTATE_SEEDS -> mutate(seeds.get(rnd.nextInt(seeds.size())), rnd);
                case COVERAGE_GUIDED -> mutate(corpus.get(rnd.nextInt(corpus.size())), rnd);
            };
            Coverage cov = new Coverage();
            String signature = execute(input, cov);
            if (signature != null && !crashes.containsKey(signature)) {
                crashes.put(signature, new Crash(signature, input, minimize(input, signature), n));
            }
            BitSet fresh = (BitSet) cov.branches().clone();
            fresh.andNot(seen);
            if (!fresh.isEmpty()) {
                seen.or(cov.branches());
                if (strategy == Strategy.COVERAGE_GUIDED) {
                    corpus.add(input);
                }
            }
        }
        return new Result(strategy, iterations, seen.cardinality(), corpus.size(), List.copyOf(crashes.values()));
    }

    /** Runs the parser once; returns null for a clean parse or an allowed ParseException, otherwise a signature that names the kind of failure. */
    String execute(String input, Coverage cov) {
        try {
            parser.parse(input, cov);
            return null;
        } catch (ParseException e) {
            return null;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + " after branch " + cov.last();
        }
    }

    /** Delta debugging, simplified: drop chunks of the input, big ones first, as long as the same failure remains. */
    String minimize(String input, String signature) {
        String best = input;
        for (int chunk = Math.max(1, best.length() / 2); chunk >= 1; chunk /= 2) {
            boolean shrunk = true;
            while (shrunk) {
                shrunk = false;
                for (int start = 0; start + chunk <= best.length(); start += chunk) {
                    String candidate = best.substring(0, start) + best.substring(start + chunk);
                    if (signature.equals(execute(candidate, new Coverage()))) {
                        best = candidate;
                        shrunk = true;
                        break;
                    }
                }
            }
        }
        return best;
    }

    private static String randomText(SplittableRandom rnd) {
        int len = rnd.nextInt(40);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < len; i++) {
            b.append(rnd.nextInt(10) == 0 ? '\n' : (char) (32 + rnd.nextInt(95)));
        }
        return b.toString();
    }

    private static String mutate(String base, SplittableRandom rnd) {
        StringBuilder b = new StringBuilder(base);
        int rounds = 1 + rnd.nextInt(3);
        for (int r = 0; r < rounds; r++) {
            int at = b.length() == 0 ? 0 : rnd.nextInt(b.length() + 1);
            switch (rnd.nextInt(6)) {
                case 0 -> { // change one character
                    if (b.length() > 0) {
                        b.setCharAt(Math.min(at, b.length() - 1), (char) (32 + rnd.nextInt(95)));
                    }
                }
                case 1 -> b.insert(at, (char) (32 + rnd.nextInt(95))); // insert a character
                case 2 -> { // delete a few characters
                    int len = Math.min(1 + rnd.nextInt(4), b.length() - at);
                    if (len > 0) {
                        b.delete(at, at + len);
                    }
                }
                case 3 -> b.insert(at, DICTIONARY[rnd.nextInt(DICTIONARY.length)]); // a token the format uses
                case 4 -> { // repeat a piece
                    if (b.length() > 0) {
                        int from = rnd.nextInt(b.length());
                        int to = Math.min(b.length(), from + 1 + rnd.nextInt(8));
                        b.insert(at, b.substring(from, to));
                    }
                }
                default -> b.insert(at, "\n"); // a line break
            }
        }
        return b.toString();
    }
}
