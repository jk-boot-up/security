package com.jk.explore.security.mtls;

import java.util.Map;
import java.util.Set;

/** Which service identity may call which endpoint. */
final class Policy {

    private final Map<String, Set<String>> rules;

    Policy(Map<String, Set<String>> rules) {
        this.rules = Map.copyOf(rules);
    }

    /** The ledger's rules: payments moves money, reporting reads statements. */
    static Policy ledger() {
        return new Policy(Map.of(
                "POST /transfers", Set.of("payments"),
                "GET /statements", Set.of("reporting")));
    }

    boolean knows(String method, String path) {
        return rules.containsKey(method + " " + path);
    }

    boolean allows(String identity, String method, String path) {
        return rules.getOrDefault(method + " " + path, Set.of()).contains(identity);
    }
}
