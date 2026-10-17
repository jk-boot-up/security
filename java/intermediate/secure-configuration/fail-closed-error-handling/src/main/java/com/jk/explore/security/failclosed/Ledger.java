package com.jk.explore.security.failclosed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Account balances in cents. A frozen account refuses credits, which is the failure the transfers meet halfway. */
final class Ledger {

    static final class FrozenAccountException extends RuntimeException {
        FrozenAccountException(String account) {
            super("account " + account + " is frozen");
        }
    }

    private final Map<String, Long> balances = new LinkedHashMap<>();
    private final Set<String> frozen;

    Ledger(Map<String, Long> opening, Set<String> frozen) {
        balances.putAll(opening);
        this.frozen = Set.copyOf(frozen);
    }

    long balance(String account) {
        return balances.get(account);
    }

    long total() {
        return balances.values().stream().mapToLong(Long::longValue).sum();
    }

    boolean exists(String account) {
        return balances.containsKey(account);
    }

    boolean acceptsCredit(String account) {
        return exists(account) && !frozen.contains(account);
    }

    void debit(String account, long cents) {
        long now = balances.get(account);
        if (now < cents) {
            throw new IllegalStateException("insufficient funds");
        }
        balances.put(account, now - cents);
    }

    void credit(String account, long cents) {
        if (frozen.contains(account)) {
            throw new FrozenAccountException(account);
        }
        balances.merge(account, cents, Long::sum);
    }
}
