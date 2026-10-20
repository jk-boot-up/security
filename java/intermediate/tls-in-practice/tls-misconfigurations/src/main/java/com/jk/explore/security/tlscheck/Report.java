package com.jk.explore.security.tlscheck;

import java.util.List;

/** The findings for one server, worst first, and a grade from A to F. */
public record Report(String host, List<Finding> findings, int score, char grade) {

    public boolean has(String rule) {
        return findings.stream().anyMatch(f -> f.rule().equals(rule));
    }

    public long count(Severity s) {
        return findings.stream().filter(f -> f.severity() == s).count();
    }
}
