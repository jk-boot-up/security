package com.jk.explore.security.workloadid;

import java.util.List;

/** The ledger's authorization: which SPIFFE ID may call which route. Anything not listed is refused. */
final class Policy {

    /** One rule: this identity may make this request. */
    record Rule(String callerId, String method, String path) {
    }

    static final String DOMAIN = "spiffe://bank.example/";

    static final List<Rule> RULES = List.of(
            new Rule(DOMAIN + "payments", "POST", "/transfers"),
            new Rule(DOMAIN + "reporting", "GET", "/statements"));

    private Policy() {
    }

    static boolean allows(String callerId, String method, String path) {
        if (callerId == null || !callerId.startsWith(DOMAIN)) {
            return false;
        }
        return RULES.contains(new Rule(callerId, method, path));
    }
}
