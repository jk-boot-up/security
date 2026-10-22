package com.jk.explore.security.apispring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 300 customers in memory. ana owns acc-0001 and ben owns acc-0002. */
@Component
class Bank {

    static final int CUSTOMERS = 300;

    private final Map<String, Account> accounts = new LinkedHashMap<>();

    Bank() {
        for (int i = 1; i <= CUSTOMERS; i++) {
            Account a = new Account();
            a.setId("acc-%04d".formatted(i));
            a.setDisplayName(i == 1 ? "Ana" : i == 2 ? "Ben" : "Customer " + i);
            a.setEmail("customer" + i + "@bank.example");
            a.setBalanceCents(100_000L + i);
            a.setRiskScore(i % 100);
            a.setCardNumber("4111 1111 1111 %04d".formatted(i));
            a.setPinHash("sha256:" + Integer.toHexString(i * 7919));
            accounts.put(a.getId(), a);
        }
    }

    Account account(String id) {
        return accounts.get(id);
    }

    /** The account that a signed-in user owns. */
    Account ownedBy(String user) {
        return accounts.get(user.equals("ana") ? "acc-0001" : "acc-0002");
    }

    List<Account> all() {
        return new ArrayList<>(accounts.values());
    }
}
