package com.jk.explore.security.securitychaos;

import java.util.List;

/** One chaos experiment: a hypothesis about a control, a failure to inject, and what the scenario will measure. */
record Experiment(int number, String title, String dependency, Fault fault, Scenario scenario, String hypothesis, boolean alertExpected) {

    /** What is done while the failure is in place, and what is counted. */
    enum Scenario { AUTHORIZATION, FRAUD, AUDIT_BURST, AUDIT_SMALL, RATE_BURST, REVOCATION }

    String label() {
        return "E" + number;
    }

    static List<Experiment> all() {
        return List.of(
                new Experiment(1, "authorization service down", Backends.AUTHZ, Fault.DOWN, Scenario.AUTHORIZATION,
                        "no payment is approved while authorization is down", true),
                new Experiment(2, "authorization service slow", Backends.AUTHZ, Fault.LATENCY, Scenario.AUTHORIZATION,
                        "none is approved when authorization answers too late", true),
                new Experiment(3, "authorization connection reset", Backends.AUTHZ, Fault.RESET, Scenario.AUTHORIZATION,
                        "none is approved when the authorization link is reset", true),
                new Experiment(4, "fraud service hangs", Backends.FRAUD, Fault.TIMEOUT, Scenario.FRAUD,
                        "no payment is approved without a fraud score", true),
                new Experiment(5, "fraud service on a trickle", Backends.FRAUD, Fault.BANDWIDTH, Scenario.FRAUD,
                        "none is approved without a score when it trickles", true),
                new Experiment(6, "audit sink down during a burst", Backends.AUDIT, Fault.DOWN, Scenario.AUDIT_BURST,
                        "every decision reaches the audit sink once it is back", true),
                new Experiment(7, "audit sink closes slowly", Backends.AUDIT, Fault.SLOW_CLOSE, Scenario.AUDIT_SMALL,
                        "a slow close loses no record and raises no false alarm", false),
                new Experiment(8, "rate-limit store resets connections", Backends.RATE, Fault.RESET, Scenario.RATE_BURST,
                        "one account never gets more than " + BankPayments.LIMIT + " approved in a burst", true),
                new Experiment(9, "revocation responder down", Backends.OCSP, Fault.DOWN, Scenario.REVOCATION,
                        "a revoked certificate is never approved", true),
                new Experiment(10, "revocation responder slow", Backends.OCSP, Fault.LATENCY, Scenario.REVOCATION,
                        "a revoked certificate is never approved when it is late", true));
    }
}
