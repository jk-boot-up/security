package com.jk.explore.security.sqlihibernate;

import java.util.Locale;

/** The only things a customer may sort by. A sort key cannot be a bind parameter, so it is chosen from a fixed list instead. */
public enum SortColumn {
    NAME("p.name", "name"), PRICE("p.price", "price");

    private final String jpql;
    private final String column;

    SortColumn(String jpql, String column) {
        this.jpql = jpql;
        this.column = column;
    }

    String jpql() {
        return jpql;
    }

    String column() {
        return column;
    }

    /** Anything not on the list gets the default, not an error message that would teach an attacker the schema. */
    public static SortColumn parse(String text) {
        for (SortColumn c : values()) {
            if (c.name().equals(text == null ? "" : text.toUpperCase(Locale.ROOT))) {
                return c;
            }
        }
        return NAME;
    }
}
