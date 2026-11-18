package com.jk.explore.security.leaks;

import java.util.stream.IntStream;

/** The check digit that card numbers carry. A run of digits that passes it is very probably a card number, so the redactor masks it wherever it appears. */
final class Luhn {

    private Luhn() {
    }

    /** What each digit adds to the sum: from the right, every second digit is doubled, and 9 is taken off a result over 9. */
    static int[] contributions(String digits) {
        int[] c = new int[digits.length()];
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            c[i] = d;
            alternate = !alternate;
        }
        return c;
    }

    static int sum(String digits) {
        return IntStream.of(contributions(digits)).sum();
    }

    static boolean valid(String digits) {
        return sum(digits) % 10 == 0;
    }
}
