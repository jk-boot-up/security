package com.jk.explore.security.sqli;

import java.util.Set;

/** The shop's database: public products, hidden products, customer accounts and orders. Fixed data, so every run is the same. */
public final class Shop {

    static final String SECRET_HASH = "pbkdf2$ana$9f2c";

    private Shop() {
    }

    public static MiniDb database() {
        MiniDb db = new MiniDb();
        db.createTable("products", "name", "price", "listed");
        db.insert("products", "Blue Kettle", 2500L, 1L);
        db.insert("products", "Red Kettle", 2900L, 1L);
        db.insert("products", "Copper Pan", 4200L, 1L);
        db.insert("products", "Prototype Toaster", 9900L, 0L); // not for sale yet
        db.createTable("users", "email", "password_hash");
        db.insert("users", "ana@shop.example", SECRET_HASH);
        db.insert("users", "ben@shop.example", "pbkdf2$ben$41d0");
        db.createTable("orders", "customer", "item", "total");
        db.insert("orders", "ana@shop.example", "Copper Pan", 4200L);
        db.insert("orders", "ben@shop.example", "Blue Kettle", 2500L);
        return db;
    }

    /** The database user the search page runs as: everything, as in a first version where one account does all the work. */
    public static MiniDb.Connection adminConnection(MiniDb db) {
        return db.connect(Set.of("products", "users", "orders"));
    }

    /** A user that may only read the product table: all the search page needs. */
    public static MiniDb.Connection searchConnection(MiniDb db) {
        return db.connect(Set.of("products"));
    }
}
