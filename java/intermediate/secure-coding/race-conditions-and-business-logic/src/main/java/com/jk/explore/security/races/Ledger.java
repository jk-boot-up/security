package com.jk.explore.security.races;

import java.util.HashMap;
import java.util.Map;

/**
 * The shared state of the step-by-step model: account balances, what has been paid out, and which request holds which lock.
 * It is only ever used by one thread, the {@link Interleaving}, so its own methods need no locks; the locks it records are the
 * ones the modelled requests take.
 */
final class Ledger {

    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, Long> paidOut = new HashMap<>();
    private final Map<String, String> lockOwners = new HashMap<>();

    Ledger open(String account, long balance) {
        balances.put(account, balance);
        return this;
    }

    long balance(String account) {
        return balances.get(account);
    }

    void set(String account, long balance) {
        balances.put(account, balance);
    }

    /** Sets the balance only if it is still the value read earlier, in one step: a compare-and-set. */
    boolean compareAndSet(String account, long expected, long updated) {
        if (balances.get(account) != expected) {
            return false;
        }
        balances.put(account, updated);
        return true;
    }

    void payOut(String account, long amount) {
        paidOut.merge(account, amount, Long::sum);
    }

    long paidOut(String account) {
        return paidOut.getOrDefault(account, 0L);
    }

    /** Takes a lock if it is free or already held by the same request. */
    boolean tryLock(String lock, String owner) {
        String current = lockOwners.putIfAbsent(lock, owner);
        return current == null || current.equals(owner);
    }

    void unlock(String lock, String owner) {
        lockOwners.remove(lock, owner);
    }
}
