package com.jk.explore.security.attacks;

/** What the bank's login answered to one attempt. */
public enum Outcome {
    /** Password right, and nothing more was needed: the attacker is in. */
    OK,
    WRONG_PASSWORD,
    UNKNOWN_USER,
    /** The account is locked after too many failures. */
    LOCKED,
    /** This address has made too many attempts in the current window. */
    RATE_LIMITED,
    /** The password was right but a second factor, which the attacker lacks, is still needed. */
    SECOND_FACTOR_NEEDED,
    /** The password was right but is known from a breach, so the customer must choose a new one. */
    RESET_REQUIRED
}
