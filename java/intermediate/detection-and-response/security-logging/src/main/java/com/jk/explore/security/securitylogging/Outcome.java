package com.jk.explore.security.securitylogging;

import java.util.Locale;

/** How an event ended. */
enum Outcome {
    SUCCESS,
    FAILURE,
    DENIED;

    String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
