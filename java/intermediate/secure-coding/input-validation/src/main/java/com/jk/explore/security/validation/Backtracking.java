package com.jk.explore.security.validation;

/**
 * A model of how a backtracking regular-expression engine (such as java.util.regex) runs the pattern {@code (a+)+$}: the inner and
 * outer repetitions can split a run of a's in every possible way, and on an input that fails at the last character the engine tries
 * them all. The model counts the steps, so the cost can be measured exactly instead of by timing.
 */
public final class Backtracking {

    private long steps;

    /** Matches {@code (a+)+$} the way a backtracking engine does, counting one step for each character a group takes. */
    public boolean nested(String input) {
        steps = 0;
        return group(input, 0);
    }

    private boolean group(String s, int start) {
        int end = start;
        while (end < s.length() && s.charAt(end) == 'a') {
            end++;
        }
        for (int stop = end; stop > start; stop--) {
            steps += stop - start;
            if (stop == s.length() || group(s, stop)) {
                return true;
            }
        }
        return false;
    }

    /** The same language, one or more a's and nothing else, checked in one pass. */
    public boolean linear(String input) {
        steps = 0;
        for (int i = 0; i < input.length(); i++) {
            steps++;
            if (input.charAt(i) != 'a') {
                return false;
            }
        }
        return !input.isEmpty();
    }

    public long steps() {
        return steps;
    }
}
