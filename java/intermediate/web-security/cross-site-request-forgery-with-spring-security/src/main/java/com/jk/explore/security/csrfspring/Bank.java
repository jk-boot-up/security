package com.jk.explore.security.csrfspring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** The bank's accounts. Fixed starting balances, so every run is the same. */
@Component
class Bank {

    private final Map<String, Long> balances = new ConcurrentHashMap<>(Map.of("ana", 1000L, "mallory", 0L));

    long balance(String account) {
        return balances.getOrDefault(account, 0L);
    }

    String transfer(String from, String to, long amount) {
        if (amount <= 0 || balance(from) < amount || !balances.containsKey(to)) {
            throw new IllegalArgumentException("transfer refused");
        }
        balances.merge(from, -amount, Long::sum);
        balances.merge(to, amount, Long::sum);
        return "sent " + amount + " to " + to;
    }
}
