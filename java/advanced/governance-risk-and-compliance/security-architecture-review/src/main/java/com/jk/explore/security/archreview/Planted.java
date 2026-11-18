package com.jk.explore.security.archreview;

import java.util.List;

/** A weakness put in the first design on purpose: what it is, which rule should report it and on what. A rule of {@code null} means no rule can. */
public record Planted(String description, String rule, String subject) {

    public static final List<Planted> FIRST_DESIGN = List.of(
            new Planted("web reads the ledger database directly", "no-direct-path-to-data", "web -> ledgerdb"),
            new Planted("sessions cross a zone unencrypted", "cross-zone-encrypted", "web -> sessions"),
            new Planted("card network calls check no caller", "cross-zone-authenticated", "api -> cards"),
            new Planted("ledger calls inside a zone are unprotected", "every-flow-encrypted-and-authenticated", "api -> ledger"),
            new Planted("confidential data unencrypted at rest", "confidential-encrypted-at-rest", "ledgerdb"),
            new Planted("the reporting job has no owner", "has-owner", "reports"),
            new Planted("the confidential store has no decision", "confidential-has-decision", "ledgerdb"),
            new Planted("sessions rest on a superseded decision", "decision-is-accepted", "sessions"),
            new Planted("the card link rests on a proposed decision", "decision-is-accepted", "cards"),
            new Planted("web trusts a hidden account-number field", null, "web"));

    boolean reportedBy(List<Finding> findings) {
        return findings.stream()
                .anyMatch(f -> f.rule().equals(rule) && f.subject().equals(subject));
    }
}
