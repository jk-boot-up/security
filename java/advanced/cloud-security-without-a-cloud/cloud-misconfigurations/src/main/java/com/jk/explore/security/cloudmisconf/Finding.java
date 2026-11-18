package com.jk.explore.security.cloudmisconf;

import java.util.List;

/** One misconfiguration: a rule, how bad it is, the resource, and why it matters. */
public record Finding(String rule, String severity, String resource, String why) {

    /** The severities, worst first. */
    public static final List<String> SEVERITIES = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW");

    public int rank() {
        return SEVERITIES.indexOf(severity);
    }

    @Override
    public String toString() {
        return String.format("%-9s %-26s %s", severity, rule, resource);
    }
}
