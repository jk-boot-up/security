package com.jk.explore.security.hashingspring;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Salts from a fixed seed, so the stored texts this demo prints are the same on every run and the video can show them.
 * For the demo only: real code gives the encoder {@code new SecureRandom()}, or nothing, and gets unpredictable salts.
 */
public class RepeatableSalts extends SecureRandom {

    private final Random seeded = new Random(2026);

    @Override
    public void nextBytes(byte[] bytes) {
        seeded.nextBytes(bytes);
    }
}
