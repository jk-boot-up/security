package com.jk.explore.security.accesscontrolspring;

import java.util.List;

/** The store's people and orders: the same story as the plain project. Every account has the demo password. */
final class Catalogue {

    /** A demo value for every account. It protects nothing and is never logged. */
    static final String PASSWORD = "shop-demo-password";

    /** An order id that is hard to guess, shared by ben in a gift link. */
    static final String GIFT_ORDER = "c5e1f0a8-4b2d-4c19-9e7a-3d6b2f8a1c40";

    record Account(String name, String role) {
    }

    static final List<Account> ACCOUNTS = List.of(
            new Account("ana", "CUSTOMER"),
            new Account("ben", "CUSTOMER"),
            new Account("cara", "CUSTOMER"),
            new Account("sam", "SUPPORT"),
            new Account("dave", "ADMIN"));

    static List<Order> orders() {
        return List.of(
                new Order("1001", "ana", "laptop", "12 Palm Street"),
                new Order("1002", "ben", "headphones", "7 Lake Road"),
                new Order("1003", "ana", "keyboard", "12 Palm Street"),
                new Order("1004", "cara", "monitor", "3 Hill Lane"),
                new Order(GIFT_ORDER, "ben", "gift card", "7 Lake Road"));
    }

    private Catalogue() {
    }
}
