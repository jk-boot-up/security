package com.jk.explore.security.pinning;

/** What the client did with a server's chain. */
public enum Outcome {
    ACCEPTED, REFUSED_BY_VALIDATION, REFUSED_BY_PIN, ACCEPTED_REPORT_ONLY
}
