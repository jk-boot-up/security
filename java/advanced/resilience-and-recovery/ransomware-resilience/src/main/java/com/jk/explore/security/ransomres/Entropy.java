package com.jk.explore.security.ransomres;

/** Shannon entropy in bits per byte: about 4 to 5 for a text ledger, close to 8 for encrypted or compressed data. */
final class Entropy {

    static final int BLOCK = 4096;
    /** A block or a file above this is not a ledger. */
    static final double LIMIT = 7.5;

    private Entropy() {
    }

    static double bits(byte[] data, int from, int length) {
        if (length <= 0) {
            return 0;
        }
        int[] counts = new int[256];
        for (int i = from; i < from + length; i++) {
            counts[data[i] & 0xff]++;
        }
        double h = 0;
        for (int c : counts) {
            if (c > 0) {
                double p = (double) c / length;
                h -= p * (Math.log(p) / Math.log(2));
            }
        }
        return h;
    }

    static double bits(byte[] data) {
        return bits(data, 0, data.length);
    }

    /** The highest entropy of any full 4 KiB block (blocks shorter than half of that are too small to tell). */
    static double maxBlock(byte[] data) {
        double max = 0;
        for (int off = 0; off < data.length; off += BLOCK) {
            int len = Math.min(BLOCK, data.length - off);
            if (len >= BLOCK / 2) {
                max = Math.max(max, bits(data, off, len));
            }
        }
        return max;
    }
}
