package com.jk.explore.security.tlslog;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/** A repeatable byte stream from a label (SHA-256 in counter mode). It exists only so that the demo prints the same text every time. Never use it for real keys. */
final class LabelledRandom extends SecureRandom {
    private static final long serialVersionUID = 1L;

    private final byte[] label;
    private long counter;

    LabelledRandom(String label) {
        this.label = label.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void nextBytes(byte[] out) {
        int pos = 0;
        while (pos < out.length) {
            byte[] block = Hkdf.sha256(label, new byte[] {(byte) (counter >> 24), (byte) (counter >> 16), (byte) (counter >> 8), (byte) counter});
            counter++;
            int n = Math.min(block.length, out.length - pos);
            System.arraycopy(block, 0, out, pos, n);
            pos += n;
        }
    }
}
