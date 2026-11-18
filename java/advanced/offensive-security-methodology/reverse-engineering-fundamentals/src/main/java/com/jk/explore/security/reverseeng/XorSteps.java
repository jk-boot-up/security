package com.jk.explore.security.reverseeng;

import java.util.ArrayList;
import java.util.List;

/** Redoes by hand what the obfuscated client's decoder (a.c.d) does: two hex digits make a byte, and XOR 0x5A gives the character. */
public final class XorSteps {

    /** The key the decoder XORs every byte with; a reader finds it in the decoder's bytecode. */
    public static final int KEY = 0x5A;

    /** One byte of an encoded string: its two hex digits, their value, and the value after XOR. */
    public record Step(String hex, int encoded, int decoded) {

        public char character() {
            return (char) decoded;
        }
    }

    private XorSteps() {
    }

    public static List<Step> steps(String hex) {
        List<Step> out = new ArrayList<>();
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            int encoded = Integer.parseInt(hex.substring(i, i + 2), 16);
            out.add(new Step(hex.substring(i, i + 2), encoded, encoded ^ KEY));
        }
        return out;
    }

    public static String decode(String hex) {
        StringBuilder out = new StringBuilder();
        steps(hex).forEach(s -> out.append(s.character()));
        return out.toString();
    }
}
