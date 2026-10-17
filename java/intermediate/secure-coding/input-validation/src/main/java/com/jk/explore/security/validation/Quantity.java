package com.jk.explore.security.validation;

/** How many of one item: a whole number from 1 to 99, written with digits only. */
public record Quantity(int value) {

    public static final int MAX = 99;

    public static Quantity parse(String raw) throws Rejected {
        String text = Text.bounded("quantity", raw, 3);
        if (text.isEmpty() || !text.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new Rejected("quantity", "digits only");
        }
        int value = Integer.parseInt(text);
        if (value < 1 || value > MAX) {
            throw new Rejected("quantity", "from 1 to " + MAX);
        }
        return new Quantity(value);
    }
}
