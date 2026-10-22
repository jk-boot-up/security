package com.jk.explore.security.hostscan;

/** One thing the scan found: how bad it is, which rule, where, and why it matters. {@code fix} is a command that repairs it mechanically, or null when a person must decide. */
record Finding(Severity severity, String rule, String where, String why, String fix) {

    enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    boolean autoFixable() {
        return fix != null;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-22s %s: %s%s", severity, rule, where, why, autoFixable() ? " [auto-fixable]" : "");
    }
}
