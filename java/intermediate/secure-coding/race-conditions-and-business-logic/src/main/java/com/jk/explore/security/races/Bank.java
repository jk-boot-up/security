package com.jk.explore.security.races;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bank's transfer service. Every transfer passes the business rules (a positive amount, two different known accounts, enough
 * money), runs as one unit under the bank's lock, and can carry an idempotency key: a request sent again with the same key, as a
 * client does when a reply is lost, returns the first answer instead of moving the money again.
 */
final class Bank {

    record Transfer(String from, String to, long amount) {
    }

    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, Integer> rewardPoints = new HashMap<>();
    private final ConcurrentHashMap<String, Transfer> keys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> answers = new ConcurrentHashMap<>();
    private final boolean rules;

    /** A bank with the business rules switched on, or off for the demo's careless version. */
    Bank(boolean rules) {
        this.rules = rules;
    }

    synchronized Bank open(String account, long balance) {
        balances.put(account, balance);
        return this;
    }

    synchronized long balance(String account) {
        return balances.get(account);
    }

    synchronized int rewardPoints(String account) {
        return rewardPoints.getOrDefault(account, 0);
    }

    /** One transfer, with no key: sending it twice moves the money twice. A transfer earns its sender one reward point. */
    synchronized String transfer(Transfer t) {
        if (rules) {
            String broken = brokenRule(t);
            if (broken != null) {
                return "refused: " + broken;
            }
        }
        balances.put(t.from(), balances.get(t.from()) - t.amount());
        balances.put(t.to(), balances.get(t.to()) + t.amount());
        rewardPoints.merge(t.from(), 1, Integer::sum);
        return "moved " + t.amount();
    }

    /**
     * A transfer with an idempotency key. The key is claimed in one step before anything happens, so two copies of the same
     * request racing each other cannot both run. The same key with a different transfer is refused: a key names one request.
     */
    String transfer(String key, Transfer t) {
        Transfer earlier = keys.putIfAbsent(key, t);
        if (earlier != null) {
            if (!earlier.equals(t)) {
                return "refused: the key " + key + " was used for a different transfer";
            }
            String answer;
            while ((answer = answers.get(key)) == null) {
                Thread.onSpinWait();
            }
            return "already done: " + answer;
        }
        String answer = "failed";
        try {
            answer = transfer(t);
            return answer;
        } finally {
            answers.put(key, answer);
        }
    }

    private String brokenRule(Transfer t) {
        if (t.amount() <= 0) {
            return "the amount must be above zero";
        }
        if (t.from().equals(t.to())) {
            return "a transfer to the same account";
        }
        if (!balances.containsKey(t.from()) || !balances.containsKey(t.to())) {
            return "an unknown account";
        }
        if (balances.get(t.from()) < t.amount()) {
            return "not enough money";
        }
        return null;
    }
}
