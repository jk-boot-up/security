package com.jk.explore.security.fuzzing;

/**
 * Delta debugging: removes pieces of a crashing input for as long as it still crashes the same way, first in large pieces and then in
 * smaller ones, until no single character can be removed. The result is the smallest input that shows the bug, ready to be a test.
 */
final class Minimizer {

    private Minimizer() {
    }

    static String minimise(Harness harness, String input) {
        Throwable first = Fuzzer.execute(harness, input).failure();
        if (first == null) {
            throw new IllegalArgumentException("the input does not crash");
        }
        String signature = Crash.signature(first);
        String current = input;
        int pieces = 2;
        while (current.length() >= 2) {
            int size = (int) Math.ceil(current.length() / (double) pieces);
            boolean removed = false;
            for (int start = 0; start < current.length(); start += size) {
                String candidate = current.substring(0, start) + current.substring(Math.min(current.length(), start + size));
                if (sameCrash(harness, candidate, signature)) {
                    current = candidate;
                    pieces = Math.max(pieces - 1, 2);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                if (size == 1) {
                    break;
                }
                pieces = Math.min(current.length(), pieces * 2);
            }
        }
        return current;
    }

    private static boolean sameCrash(Harness harness, String input, String signature) {
        Throwable failure = Fuzzer.execute(harness, input).failure();
        return failure != null && Crash.signature(failure).equals(signature);
    }
}
