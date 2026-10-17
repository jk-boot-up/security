package com.jk.explore.security.auth;

import java.util.HashMap;
import java.util.Map;

/** Where the current role of every account is kept, and how many times it has been looked up. */
public class UserDirectory {

    private final Map<String, Account> accounts = new HashMap<>();
    private int lookups;

    public UserDirectory add(Account account) {
        accounts.put(account.username(), account);
        return this;
    }

    public Account find(String username) {
        lookups++;
        return accounts.get(username);
    }

    public void changeRole(String username, Role role) {
        accounts.put(username, new Account(username, role));
    }

    public int lookups() {
        return lookups;
    }
}
