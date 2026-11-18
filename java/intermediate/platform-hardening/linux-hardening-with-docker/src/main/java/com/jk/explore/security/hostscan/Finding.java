package com.jk.explore.security.hostscan;

/** One thing the scan found: how bad it is, which rule, where, and why it matters. {@code fix} is a command that repairs it mechanically, or null when a person must decide.
 * The report line shows the rule and where; {@code auto} marks a finding with a fix. */
record Finding(Severity severity, String rule, String where, String why, String fix) {

    enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    boolean autoFixable() {
        return fix != null;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-19s %s%s", severity, rule, where, autoFixable() ? "  auto" : "");
    }
}
