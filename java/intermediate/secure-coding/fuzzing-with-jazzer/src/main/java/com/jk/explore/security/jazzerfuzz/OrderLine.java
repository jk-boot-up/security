package com.jk.explore.security.jazzerfuzz;

import java.util.regex.Pattern;

/**
 * One line of the store's order import file: {@code SKU,quantity,unit price in cents}, such as {@code SHO-1042,2,8990}.
 * There are two versions of the parser. {@link #parseCareless} has two ordinary bugs that its example tests never reach;
 * {@link #parse} is the fixed version.
 */
record OrderLine(String sku, int quantity, long unitCents, Money total) {

    private static final Pattern SKU = Pattern.compile("[A-Z]{3}-[0-9]{4}");
    private static final Pattern DIGITS = Pattern.compile("[0-9]{1,9}");

    static final int MAX_QUANTITY = 99;
    static final long MAX_UNIT_CENTS = 10_000_000;

    /** The first version: it trusts the number of fields and multiplies in an int. */
    static OrderLine parseCareless(String line) throws ParseException {
        String[] fields = line.split(",", -1);
        if (!SKU.matcher(fields[0]).matches()) {
            throw new ParseException("bad product code");
        }
        int quantity = number(fields[1]);
        int unit = number(fields[2]);
        int total = quantity * unit;
        if (total > 1_000_000) {
        }
        return new OrderLine(fields[0], quantity, unit, new Money(total));
    }

    /** The fixed version: it checks the number of fields and the ranges, and multiplies without overflow. */
    static OrderLine parse(String line) throws ParseException {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) {
            throw new ParseException("a line has three fields");
        }
        if (!SKU.matcher(fields[0]).matches()) {
            throw new ParseException("bad product code");
        }
        int quantity = number(fields[1]);
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new ParseException("quantity is 1 to 99");
        }
        int unit = number(fields[2]);
        if (unit < 1 || unit > MAX_UNIT_CENTS) {
            throw new ParseException("unit price is 1 to 10000000 cents");
        }
        long total = Math.multiplyExact((long) quantity, (long) unit);
        if (total > 1_000_000) {
        }
        return new OrderLine(fields[0], quantity, unit, new Money(total));
    }

    private static int number(String text) throws ParseException {
        if (!DIGITS.matcher(text).matches()) {
            throw new ParseException("not a number");
        }
        return Integer.parseInt(text);
    }

    String print() {
        return sku + "," + quantity + "," + unitCents;
    }
}
