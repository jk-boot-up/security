package com.jk.explore.security.validation;

import java.util.regex.Pattern;

/** A product code: three capital letters, a dash and four digits, such as SHO-1042. Nothing else is a product code. */
public record Sku(String value) {

    private static final Pattern SHAPE = Pattern.compile("[A-Z]{3}-[0-9]{4}");

    public static Sku parse(String raw) throws Rejected {
        String text = Text.bounded("sku", raw, 8);
        if (!SHAPE.matcher(text).matches()) {
            throw new Rejected("sku", "three capital letters, a dash and four digits");
        }
        return new Sku(text);
    }
}
