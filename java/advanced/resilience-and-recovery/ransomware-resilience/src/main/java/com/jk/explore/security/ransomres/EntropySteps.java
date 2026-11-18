package com.jk.explore.security.ransomres;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The entropy of one block worked out term by term, as the guard computes it: H = the sum, over every byte value that occurs,
 * of -p x log2(p), where p = count / length. A test proves the total is exactly {@link Entropy#bits(byte[])}.
 */
final class EntropySteps {

    /** One byte value: how often it occurs, its share p, and its term -p x log2(p). */
    record Term(int value, int count, double p, double bits) {
    }

    /** The whole sum: the block length, how many byte values occur, the terms (largest count first) and the total in bits per byte. */
    record Worked(int length, int distinct, List<Term> terms, double total) {
    }

    private EntropySteps() {
    }

    static Worked of(byte[] block) {
        int[] counts = new int[256];
        for (byte b : block) {
            counts[b & 0xff]++;
        }
        List<Term> terms = new ArrayList<>();
        double total = 0;
        for (int value = 0; value < 256; value++) {
            if (counts[value] > 0) {
                double p = (double) counts[value] / block.length;
                double bits = -p * (Math.log(p) / Math.log(2));
                terms.add(new Term(value, counts[value], p, bits));
                total -= p * (Math.log(p) / Math.log(2));
            }
        }
        terms.sort(Comparator.comparingInt(Term::count).reversed().thenComparingInt(Term::value));
        return new Worked(block.length, terms.size(), terms, total);
    }

    /**
     * The same block as ransomware would leave it, encrypted with AES. The ransomware simulation uses a random key that is thrown
     * away; this example uses a fixed key and counter so that the printed numbers are the same on every run.
     */
    static byte[] encryptedForExample(byte[] block) throws GeneralSecurityException {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(new byte[16]));
        return cipher.doFinal(block);
    }

    /** A byte value as a reader would recognise it: a printable character in quotes, a newline as \n, anything else in hex. */
    static String show(int value) {
        if (value == '\n') {
            return "'\\n'";
        }
        return value >= 0x21 && value < 0x7f ? "'" + (char) value + "'" : String.format("0x%02x", value);
    }
}
