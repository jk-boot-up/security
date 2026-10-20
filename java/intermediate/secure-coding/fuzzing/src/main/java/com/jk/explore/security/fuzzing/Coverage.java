package com.jk.explore.security.fuzzing;

import java.util.BitSet;

/**
 * Which branches of a parser one run reached. The parsers are instrumented by hand: each interesting branch calls {@link #hit}
 * with its own number. Real tools such as Jazzer add the same kind of counters to the bytecode automatically.
 */
final class Coverage {

    private final BitSet hits = new BitSet();

    void hit(int branch) {
        hits.set(branch);
    }

    BitSet hits() {
        return (BitSet) hits.clone();
    }

    /** True if this run reached a branch that {@code seen} has not. */
    boolean addsTo(BitSet seen) {
        BitSet copy = (BitSet) hits.clone();
        copy.andNot(seen);
        return !copy.isEmpty();
    }
}
