package com.jk.explore.security.secrets;

import java.util.HashMap;
import java.util.Map;

/** The store's database, which accepts a login only for an account that exists, has not expired, and has the right password. */
final class Database {

    private record Account(Secret password, long expiresAt) {
    }

    static final long NEVER = Long.MAX_VALUE;

    private final SimClock clock;
    private final Map<String, Account> accounts = new HashMap<>();

    Database(SimClock clock) {
        this.clock = clock;
    }

    void createAccount(String user, Secret password, long expiresAt) {
        accounts.put(user, new Account(password, expiresAt));
    }

    void extend(String user, long expiresAt) {
        Account account = accounts.get(user);
        if (account != null) {
            accounts.put(user, new Account(account.password(), expiresAt));
        }
    }

    void dropAccount(String user) {
        accounts.remove(user);
    }

    boolean login(String user, Secret password) {
        Account account = accounts.get(user);
        if (account == null || clock.now() >= account.expiresAt()) {
            return false;
        }
        return account.password().matches(password);
    }

    int accountCount() {
        return accounts.size();
    }
}
