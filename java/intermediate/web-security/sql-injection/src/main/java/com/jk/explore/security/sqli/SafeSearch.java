package com.jk.explore.security.sqli;

import java.util.List;

/** The fixed search: the statement is constant text, the customer's words travel as a parameter, and the sort comes from a list. */
public final class SafeSearch {

    private final MiniDb.Connection db;

    public SafeSearch(MiniDb.Connection db) {
        this.db = db;
    }

    public List<MiniDb.Row> search(String term, String sort) {
        String sql = "SELECT name, price FROM products WHERE name LIKE ? AND listed = 1 ORDER BY " + SortColumn.parse(sort).column();
        return db.query(sql, "%" + term + "%");
    }
}
