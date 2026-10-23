package com.jk.explore.security.workloadid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class RotationsTest {

    private static BigInteger n(long value) {
        return BigInteger.valueOf(value);
    }

    @Test
    void sameSerialSeenManyTimesIsNoRotation() {
        assertEquals(0, Rotations.count(List.of(n(1), n(1), n(1))));
    }

    @Test
    void everyChangeOfSerialIsOneRotation() {
        assertEquals(2, Rotations.count(List.of(n(1), n(1), n(2), n(2), n(3))));
    }

    @Test
    void emptyAndSingleListsHaveNone() {
        assertEquals(0, Rotations.count(List.of()));
        assertEquals(0, Rotations.count(List.of(n(7))));
    }
}
