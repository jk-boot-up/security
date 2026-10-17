package com.jk.explore.security.signatures;

/** What a checkout service concludes about a published price list. */
public enum Verdict {
    OK,
    BAD_SIGNATURE,
    EXPIRED,
    OLD_VERSION
}
