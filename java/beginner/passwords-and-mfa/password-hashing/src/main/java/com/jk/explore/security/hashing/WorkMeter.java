package com.jk.explore.security.hashing;

/**
 * Counts hashing work in steps instead of seconds, so the demo prints the same numbers on every machine.
 * A fast hash costs one step; a slow hash costs one step for every round it repeats.
 */
public class WorkMeter {

    private long steps;

    public void add(long n) {
        steps += n;
    }

    public void reset() {
        steps = 0;
    }

    public long steps() {
        return steps;
    }
}
