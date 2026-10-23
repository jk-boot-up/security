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
                        "no payment is approved while authorization cannot be reached", true),
                new Experiment(2, "authorization service slow", Backends.AUTHZ, Fault.LATENCY, Scenario.AUTHORIZATION,
                        "no payment is approved when the authorization answer comes after the deadline", true),
                new Experiment(3, "authorization connection reset", Backends.AUTHZ, Fault.RESET, Scenario.AUTHORIZATION,
                        "no payment is approved when the authorization connection is reset", true),
                new Experiment(4, "fraud service hangs", Backends.FRAUD, Fault.TIMEOUT, Scenario.FRAUD,
                        "no payment is approved without a fraud score", true),
                new Experiment(5, "fraud service on a trickle", Backends.FRAUD, Fault.BANDWIDTH, Scenario.FRAUD,
                        "no payment is approved without a fraud score when the answer arrives too slowly", true),
                new Experiment(6, "audit sink down during a burst", Backends.AUDIT, Fault.DOWN, Scenario.AUDIT_BURST,
                        "every decision is in the audit sink once the sink is back", true),
                new Experiment(7, "audit sink closes slowly", Backends.AUDIT, Fault.SLOW_CLOSE, Scenario.AUDIT_SMALL,
                        "a slow close loses no audit record and raises no false alarm", false),
                new Experiment(8, "rate-limit store resets connections", Backends.RATE, Fault.RESET, Scenario.RATE_BURST,
                        "one account never gets more than " + BankPayments.LIMIT + " payments approved in a burst", true),
                new Experiment(9, "revocation responder down", Backends.OCSP, Fault.DOWN, Scenario.REVOCATION,
                        "a payment signed with a revoked certificate is never approved", true),
                new Experiment(10, "revocation responder slow", Backends.OCSP, Fault.LATENCY, Scenario.REVOCATION,
                        "a payment signed with a revoked certificate is never approved when the answer is late", true));
    }
}
