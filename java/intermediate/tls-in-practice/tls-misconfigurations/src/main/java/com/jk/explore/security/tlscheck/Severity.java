package com.jk.explore.security.tlscheck;

/** How bad a finding is, and how many points it costs. */
public enum Severity {
    INFO(0), LOW(2), MEDIUM(7), HIGH(20), CRITICAL(60);

    private final int penalty;

    Severity(int penalty) {
        this.penalty = penalty;
    }

    public int penalty() {
        return penalty;
    }
}
