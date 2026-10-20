package com.jk.explore.security.sqli;

import java.util.List;

/** The first version of the product search: the customer's text is pasted into the statement. */
public final class UnsafeSearch {

    private final MiniDb.Connection db;

    public UnsafeSearch(MiniDb.Connection db) {
        this.db = db;
    }

    /** Returns the name and price of every listed product that matches. */
    public List<MiniDb.Row> search(String term, String sort) {
        String sql = "SELECT name, price FROM products WHERE name LIKE '%" + term + "%' AND listed = 1 ORDER BY " + sort;
        return db.query(sql);
    }
}
