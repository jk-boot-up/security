package com.jk.explore.security.sqli;

import java.util.Locale;

/** The only columns a customer may sort by. A column name cannot be a bind parameter, so it is chosen from a fixed list instead. */
public enum SortColumn {
    NAME("name"), PRICE("price");

    private final String column;

    SortColumn(String column) {
        this.column = column;
    }

    String column() {
        return column;
    }

    /** Anything not on the list gets the default instead of an error message that would teach an attacker the schema. */
    public static SortColumn parse(String text) {
        for (SortColumn c : values()) {
            if (c.name().equals(text == null ? "" : text.toUpperCase(Locale.ROOT))) {
                return c;
            }
        }
        return NAME;
    }
}
