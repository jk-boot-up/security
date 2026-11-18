package com.jk.explore.security.passkeys;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Demo only: a SecureRandom that gives the same bytes on every run, so the challenges, keys and signatures the demo prints repeat and
 * can be checked against the golden output. It is predictable, so it must never protect anything; real code uses new SecureRandom().
 */
final class FixedRandom extends SecureRandom {

    private final Random source;

    FixedRandom(long seed) {
        this.source = new Random(seed);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        source.nextBytes(bytes);
    }
}
