package com.jk.explore.security.hashing;

import java.util.LinkedHashMap;
import java.util.Map;

/** The shop's table of accounts: a username and the stored text of its password, never the password. */
public class AccountStore {

    private final PasswordHasher hasher;
    private final Map<String, String> stored = new LinkedHashMap<>();

    public AccountStore(PasswordHasher hasher) {
        this.hasher = hasher;
    }

    public AccountStore register(String username, String password) {
        stored.put(username, hasher.hash(password));
        return this;
    }

    /** Adds an account whose stored text was made elsewhere or in the past, such as an older, weaker hash. */
    public AccountStore restore(String username, String storedText) {
        stored.put(username, storedText);
        return this;
    }

    /** The whole table, as an attacker who steals the database would see it. */
    public Map<String, String> stolen() {
        return new LinkedHashMap<>(stored);
    }

    public String storedFor(String username) {
        return stored.get(username);
    }

    /** Checks a login and, when the hasher can, quietly replaces an old, weaker hash after a correct password. */
    public Login login(String username, String password) {
        String current = stored.get(username);
        if (current == null || !hasher.verify(password, current)) {
            return Login.WRONG;
        }
        if (hasher instanceof Pbkdf2Hasher pbkdf2 && pbkdf2.needsUpgrade(current)) {
            stored.put(username, pbkdf2.hash(password));
            return Login.UPGRADED;
        }
        return Login.OK;
    }
}
