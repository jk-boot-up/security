package com.jk.explore.security.hashing;

/** The result of a login attempt. UPGRADED means it succeeded and the stored hash was replaced by a stronger one. */
public enum Login {
    OK,
    UPGRADED,
    WRONG
}
