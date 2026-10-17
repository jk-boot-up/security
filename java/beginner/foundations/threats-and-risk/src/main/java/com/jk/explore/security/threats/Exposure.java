package com.jk.explore.security.threats;

/** Who can reach an entry point: anyone on the internet, a signed-in customer, or only staff. */
public enum Exposure {
    PUBLIC,
    AUTHENTICATED,
    INTERNAL
}
