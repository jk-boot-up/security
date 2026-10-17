package com.jk.explore.security.totp;

/** The bank's answer to a login attempt. */
public enum Verdict {
    OK,
    WRONG_PASSWORD,
    /** The password was right, and a code from the second factor is still needed. */
    CODE_NEEDED,
    WRONG_CODE,
    /** The code was right, but it had already been used. */
    REPLAYED
}
