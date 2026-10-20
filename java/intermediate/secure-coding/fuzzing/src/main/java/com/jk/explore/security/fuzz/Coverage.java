package com.jk.explore.security.fuzz;

import java.util.BitSet;

/**
 * What a coverage-guided fuzzer learns from a run. A real tool (Jazzer, AFL) instruments the bytecode automatically; here the parsers call
 * {@code hit(n)} by hand at each branch, which is enough to show the idea. Each call also counts a step, so a loop that never ends is stopped.
 */
public final class Coverage {

    static final int STEP_BUDGET = 5_000;

    private final BitSet branches = new BitSet();
    private int steps;
    private int last;

    public void hit(int branch) {
        branches.set(branch);
        last = branch;
        if (++steps > STEP_BUDGET) {
            throw new Hang();
        }
    }

    /** The last branch reached: where in the parser a failure happened, even when the JVM has left out the stack trace of a hot exception. */
    public int last() {
        return last;
    }

    public BitSet branches() {
        return branches;
    }
}
