package com.jk.explore.security.asvs;

/** The three verification levels of the standard: each one includes everything below it. */
enum Level {
    L1, L2, L3;

    /** Whether a requirement written for {@code required} applies to an application verified at this level. */
    boolean covers(Level required) {
        return required.ordinal() <= ordinal();
    }
}
