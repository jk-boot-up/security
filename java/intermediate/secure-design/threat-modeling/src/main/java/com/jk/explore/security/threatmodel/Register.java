package com.jk.explore.security.threatmodel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bank's threat register: one decision for each threat key the team has looked at. A threat the rules find that has no entry
 * here is undecided, and the demo treats that as a failure, because a new threat nobody has looked at is exactly what a model is for.
 */
final class Register {

    static final int ACCEPTED_REVIEW_DAY = 90;

    private Register() {
    }

    static Map<String, Decision> bank() {
        Map<String, Decision> d = new LinkedHashMap<>();
        d.put("S:E1", Decision.mitigate(Checks.STRONG_LOGIN));
        d.put("S:E2", Decision.mitigate(Checks.MUTUAL_AUTH));
        d.put("S:P2", Decision.mitigate(Checks.RATE_LIMIT));
        d.put("S:P3", Decision.mitigate(Checks.MUTUAL_AUTH));
        for (String flow : List.of("F2", "F3", "F6", "F7", "F8")) {
            d.put("T:" + flow, Decision.mitigate(Checks.TLS));
        }
        for (String flow : List.of("F2", "F3", "F4")) {
            d.put("R:" + flow, Decision.mitigate(Checks.AUDIT));
        }
        d.put("R:F6", Decision.accept("the processor's signed receipts and the contract cover disputes", "payments lead", ACCEPTED_REVIEW_DAY));
        d.put("I:S1", Decision.mitigate(Checks.STORE_PROTECTION));
        d.put("I:S2", Decision.avoid("card numbers and secrets are never written to the audit log"));
        d.put("D:P2", Decision.transfer("the hosting provider's denial-of-service protection, under contract"));
        for (String flow : List.of("F2", "F3", "F7", "F8")) {
            d.put("E:" + flow, Decision.mitigate(Checks.AUTHORIZATION));
        }
        return d;
    }

    /** Threats found by the rules that have no decision. */
    static List<Threat> undecided(List<Threat> threats, Map<String, Decision> register) {
        return threats.stream().filter(t -> !register.containsKey(t.key())).toList();
    }

    /** Mitigations whose control has no check, so nothing proves they are in place. */
    static List<String> unproven(Map<String, Decision> register) {
        return register.entrySet().stream()
                .filter(e -> e.getValue().type() == Decision.Type.MITIGATE)
                .filter(e -> !Checks.all().containsKey(e.getValue().detail()))
                .map(Map.Entry::getKey).toList();
    }
}
