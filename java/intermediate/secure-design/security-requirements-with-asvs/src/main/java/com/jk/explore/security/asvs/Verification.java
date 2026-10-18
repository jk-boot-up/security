package com.jk.explore.security.asvs;

/** How a requirement is verified. */
enum Verification {
    /** A test that exercises the running application. */
    AUTOMATED,
    /** A check of the application's settings. */
    CONFIGURATION,
    /** A person reviews it: code review, design review or a penetration test. */
    MANUAL
}
