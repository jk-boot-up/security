package com.jk.explore.security.privacyeng;

import java.sql.SQLException;

/** How easily a person can be picked out by columns that are not names: postcode, birth date and gender. */
public final class Anonymity {

    /** The full values, and the same values made coarser: three digits of the postcode, the decade of birth. */
    public static final String FULL = "postcode, birth_date, gender";
    public static final String COARSE = "left(postcode, 3), (extract(year from birth_date)::int / 10), gender";

    private Anonymity() {
    }

    /** The share of rows of the table that sit in a group of fewer than k alike rows on the given columns. */
    public static double shareBelow(Pg pg, String table, String columns, int k) throws SQLException {
        long total = pg.count("SELECT count(*) FROM " + table);
        long below = pg.count("SELECT coalesce(sum(n), 0) FROM (SELECT count(*) AS n FROM " + table + " GROUP BY " + columns + " HAVING count(*) < ?) g", k);
        return (double) below / total;
    }

    /** The smallest group on the given columns. */
    public static long smallest(Pg pg, String table, String columns) throws SQLException {
        return pg.count("SELECT min(n) FROM (SELECT count(*) AS n FROM " + table + " GROUP BY " + columns + ") g");
    }

    /**
     * How many of the people on an outside list (a name with the three values) are found in the published rows as the only row with their values:
     * the people who can be named from data that has no names.
     */
    public static long namedFromOutside(Pg pg, String columns, int outsideShare) throws SQLException {
        return pg.count("SELECT count(*) FROM customers c WHERE c.id % 10 < ? AND (SELECT count(*) FROM customers o WHERE (o.postcode, o.birth_date, o.gender) = (c.postcode, c.birth_date, c.gender)) = 1", outsideShare);
    }

    public static long namedFromOutsideCoarse(Pg pg, int outsideShare) throws SQLException {
        return pg.count("SELECT count(*) FROM customers c WHERE c.id % 10 < ? AND (SELECT count(*) FROM customers o WHERE (left(o.postcode, 3), extract(year from o.birth_date)::int / 10, o.gender) = (left(c.postcode, 3), extract(year from c.birth_date)::int / 10, c.gender)) = 1", outsideShare);
    }
}
