package com.jk.explore.security.k8ssecrets;

import java.util.ArrayList;
import java.util.List;

/**
 * A teaching class: undoes one group of base64 by hand, to show that no key is involved.
 * Four characters are four numbers of 6 bits (their place in the alphabet), which make 24 bits, which are three bytes.
 * A test checks that the result is what {@link java.util.Base64} gives.
 */
record Base64Steps(String chars, List<Integer> sixBitValues, String sixBits, String eightBits, List<Integer> bytes, String text) {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** The first four characters of a base64 text, decoded step by step. */
    static Base64Steps firstGroup(String encoded) {
        String four = encoded.substring(0, 4);
        List<Integer> values = new ArrayList<>();
        int bits24 = 0;
        for (char c : four.toCharArray()) {
            int v = ALPHABET.indexOf(c);
            values.add(v);
            bits24 = (bits24 << 6) | v;
        }
        List<Integer> bytes = List.of(bits24 >> 16 & 0xff, bits24 >> 8 & 0xff, bits24 & 0xff);
        StringBuilder text = new StringBuilder();
        bytes.forEach(b -> text.append((char) b.intValue()));
        return new Base64Steps(four, values, String.join(" ", values.stream().map(v -> binary(v, 6)).toList()),
                String.join(" ", bytes.stream().map(b -> binary(b, 8)).toList()), bytes, text.toString());
    }

    static String binary(int value, int width) {
        String s = Integer.toBinaryString(value);
        return "0".repeat(width - s.length()) + s;
    }
}
