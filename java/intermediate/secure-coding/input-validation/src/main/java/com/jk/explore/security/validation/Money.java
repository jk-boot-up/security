package com.jk.explore.security.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** An amount of money: digits, an optional point and at most two decimals, more than zero and at most 10000.00. Never a double. */
public record Money(BigDecimal amount) {

    public static final BigDecimal MAX = new BigDecimal("10000.00");
    private static final Pattern SHAPE = Pattern.compile("[0-9]{1,5}(\\.[0-9]{1,2})?");

    public static Money parse(String raw) throws Rejected {
        String text = Text.bounded("amount", raw, 8);
        if (!SHAPE.matcher(text).matches()) {
            throw new Rejected("amount", "digits with at most two decimals");
        }
        BigDecimal amount = new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
        if (amount.signum() <= 0 || amount.compareTo(MAX) > 0) {
            throw new Rejected("amount", "more than 0 and at most " + MAX.toPlainString());
        }
        return new Money(amount);
    }
}
