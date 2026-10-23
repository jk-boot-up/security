package com.jk.explore.security.securitychaos;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bank payments service with five controls, each a call to a dependency: rate limit, certificate revocation,
 * authorization, fraud score and an audit trail. The first version fails open on every one of them; the hardened
 * version fails closed and keeps what it needs to keep going.
 */
final class BankPayments {

    static final int LIMIT = 5;
    static final int RISKY = 80;

    /** Where the five dependencies are reached (through Toxiproxy in the demo). */
    record Endpoints(String authorization, String fraud, String audit, String rateLimit, String revocation) {
    }

    private final Endpoints at;
    private final boolean hardened;
    private final Http http;
    private final AuditTrail audit;
    private final Map<String, Integer> localCounts = new ConcurrentHashMap<>();
    private final Map<String, String> knownStatus = new ConcurrentHashMap<>();

    BankPayments(Endpoints at, boolean hardened, Http http, AuditTrail audit) {
        this.at = at;
        this.hardened = hardened;
        this.http = http;
        this.audit = audit;
    }

    Decision pay(Payment p) {
        Decision d = decide(p);
        if (!audit.record(p.id())) {
            return Decision.deny("audit trail cannot keep the record");
        }
        return d;
    }

    private Decision decide(Payment p) {
        if (overLimit(p.account())) {
            return Decision.deny("rate limit");
        }
        if (!certificateGood(p.certSerial())) {
            return Decision.deny("certificate");
        }
        Optional<String> authz = http.get(at.authorization() + "/check?account=" + p.account() + "&amount=" + p.amount());
        if (authz.isEmpty()) {
            if (hardened) {
                return Decision.deny("authorization unreachable");
            }
            // first version: "the authorization service is flaky, do not block customers"
        } else if (!authz.get().equals("allow")) {
            return Decision.deny("authorization refused");
        }
        Optional<String> fraud = http.get(at.fraud() + "/score?account=" + p.account() + "&amount=" + p.amount());
        if (fraud.isEmpty()) {
            return hardened ? Decision.review("fraud score unreachable") : Decision.approve(false, "no score, treated as clean");
        }
        int score = Integer.parseInt(fraud.get().substring("score=".length()));
        return score >= RISKY ? Decision.deny("fraud score") : Decision.approve(true, "screened");
    }

    private boolean overLimit(String account) {
        Optional<String> count = http.get(at.rateLimit() + "/incr?account=" + account);
        if (count.isPresent()) {
            return Integer.parseInt(count.get().substring("count=".length())) > LIMIT;
        }
        if (!hardened) {
            return false; // no limiter, no limit
        }
        return localCounts.merge(account, 1, Integer::sum) > LIMIT;
    }

    private boolean certificateGood(String serial) {
        Optional<String> status = http.get(at.revocation() + "/status?serial=" + serial);
        if (status.isPresent()) {
            knownStatus.put(serial, status.get());
            return status.get().equals("good");
        }
        if (!hardened) {
            return true; // soft fail: no answer is read as "not revoked"
        }
        return "good".equals(knownStatus.get(serial)); // hard fail, with the last answer we saw as the only exception
    }
}
