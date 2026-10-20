package com.jk.explore.security.tlscheck;

/** One thing the checker found, with a stable rule id that a build can allow-list or fail on. */
public record Finding(String rule, Severity severity, String message) {

    @Override
    public String toString() {
        return String.format("%-8s %-22s %s", severity, rule, message);
    }
}
