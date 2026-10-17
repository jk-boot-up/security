package com.jk.explore.security.threatmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Mitigations as executable checks over the model. Each check returns what breaks it, so an empty list means it passes. When the
 * design changes and the model is updated, the same checks run again, which is how a threat model stays true.
 */
final class Checks {

    static final String TLS = "TLS on every flow that crosses a boundary";
    static final String MUTUAL_AUTH = "both ends authenticated on every flow that crosses a boundary";
    static final String AUDIT = "every process that receives money writes to the audit log";
    static final String AUTHORIZATION = "every process that receives a flow from a less trusted zone checks authorization";
    static final String STRONG_LOGIN = "customers sign in with a phishing-resistant passkey";
    static final String STORE_PROTECTION = "stores are encrypted at rest and never reached from the device or partner zones";
    static final String RATE_LIMIT = "every public-facing process checks tokens and limits rates";
    static final String OWNERS = "every data store has a named owner";

    private Checks() {
    }

    static Map<String, Function<Model, List<String>>> all() {
        Map<String, Function<Model, List<String>>> checks = new LinkedHashMap<>();
        checks.put(TLS, m -> m.flows().stream().filter(m::crosses).filter(f -> !f.encrypted())
                .map(f -> f.id() + " " + f.data() + " is not encrypted").toList());
        checks.put(MUTUAL_AUTH, m -> m.flows().stream().filter(m::crosses).filter(f -> !f.authenticated())
                .map(f -> f.id() + " " + f.data() + " is not authenticated").toList());
        checks.put(AUDIT, m -> m.flows().stream().filter(Flow::money)
                .map(Flow::to).distinct()
                .filter(id -> m.element(id).kind() == Kind.PROCESS)
                .filter(id -> m.outOf(id).stream().noneMatch(f -> f.to().equals("S2")))
                .map(id -> id + " " + m.element(id).name() + " does not write to the audit log").toList());
        checks.put(AUTHORIZATION, m -> m.flows().stream().filter(m::raisesTrust).map(Flow::to).distinct()
                .filter(id -> m.element(id).kind() == Kind.PROCESS && !m.element(id).has("authorization"))
                .map(id -> id + " " + m.element(id).name() + " does not check authorization").toList());
        checks.put(STRONG_LOGIN, m -> m.has("E1") && m.element("E1").has("passkey") ? List.of() : List.of("customers have no passkey"));
        checks.put(STORE_PROTECTION, m -> {
            List<String> broken = new ArrayList<>();
            for (Element e : m.elements()) {
                if (e.kind() != Kind.DATA_STORE) {
                    continue;
                }
                if (!e.has("encrypted-at-rest")) {
                    broken.add(e.id() + " " + e.name() + " is not encrypted at rest");
                }
                for (Flow f : m.into(e.id())) {
                    if (!m.zoneOf(f.from()).equals(BankModel.INTERNAL) && !m.zoneOf(f.from()).equals(BankModel.DMZ)) {
                        broken.add(f.id() + " reaches " + e.name() + " from the " + m.zoneOf(f.from()) + " zone");
                    }
                }
            }
            return broken;
        });
        checks.put(RATE_LIMIT, m -> m.elements().stream().filter(Element::publicFacing)
                .filter(e -> !e.has("token-check") || !e.has("rate-limit"))
                .map(e -> e.id() + " " + e.name() + " is public without token checks and rate limits").toList());
        checks.put(OWNERS, m -> m.elements().stream().filter(e -> e.kind() == Kind.DATA_STORE)
                .filter(e -> e.owner() == null || e.owner().isBlank())
                .map(e -> e.id() + " " + e.name() + " has no owner").toList());
        return checks;
    }

    /** Every check that fails, with what breaks it. */
    static Map<String, List<String>> failures(Model model) {
        Map<String, List<String>> failing = new LinkedHashMap<>();
        all().forEach((name, check) -> {
            List<String> broken = check.apply(model);
            if (!broken.isEmpty()) {
                failing.put(name, broken);
            }
        });
        return failing;
    }
}
