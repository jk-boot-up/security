package com.jk.explore.security.leaks;

/** The check digit that card numbers carry. A run of digits that passes it is very probably a card number, so the redactor masks it wherever it appears. */
final class Luhn {

    private Luhn() {
    }

    static boolean valid(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
