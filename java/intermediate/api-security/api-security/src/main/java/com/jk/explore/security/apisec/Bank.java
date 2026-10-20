package com.jk.explore.security.apisec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The bank's data: 300 accounts with fixed contents. Ana owns acc-0001. */
public final class Bank {

    static final int ACCOUNTS = 300;

    private final List<Account> accounts = new ArrayList<>();

    private Bank() {
    }

    public static Bank demo() {
        Bank b = new Bank();
        for (int i = 1; i <= ACCOUNTS; i++) {
            String id = String.format("acc-%04d", i);
            String owner = i == 1 ? "ana" : "user" + i;
            b.accounts.add(new Account(id, owner, owner, owner + "@bank.example", String.format("4242 4242 4242 %04d", i)));
        }
        return b;
    }

    public Optional<Account> find(String id) {
        return accounts.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    public List<Account> all() {
        return accounts;
    }
}
