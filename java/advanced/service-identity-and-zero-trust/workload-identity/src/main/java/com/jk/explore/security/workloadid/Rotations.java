package com.jk.explore.security.workloadid;

import java.math.BigInteger;
import java.util.List;

/** Counts how often a certificate was replaced, from the serial numbers seen one after the other. */
final class Rotations {

    private Rotations() {
    }

    /** The number of times the serial number changed. */
    static int count(List<BigInteger> seenInOrder) {
        int changes = 0;
        for (int i = 1; i < seenInOrder.size(); i++) {
            if (!seenInOrder.get(i).equals(seenInOrder.get(i - 1))) {
                changes++;
            }
        }
        return changes;
    }
}
