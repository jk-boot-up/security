package com.jk.explore.security.sqlihibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** No database needed: the allow-list for the sort key. */
class SortColumnTest {

    @Test
    void knownColumnsAreAccepted() {
        assertEquals(SortColumn.PRICE, SortColumn.parse("price"));
        assertEquals(SortColumn.NAME, SortColumn.parse("NAME"));
    }

    @Test
    void anythingElseGetsTheDefault() {
        assertEquals(SortColumn.NAME, SortColumn.parse("price; drop table products"));
        assertEquals(SortColumn.NAME, SortColumn.parse("p.passwordHash"));
        assertEquals(SortColumn.NAME, SortColumn.parse(null));
    }
}
