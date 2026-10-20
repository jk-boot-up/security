package com.jk.explore.security.pinning;

import java.util.Set;

/**
 * The pins an app ships with: at least one key in the chain must match one of them. Pinning the chain's keys rather than the leaf lets the server renew its
 * leaf; a backup pin for a key that is not in use yet lets it switch keys without an app update. {@code reportOnly} logs a mismatch instead of refusing.
 */
public record PinPolicy(Set<String> pins, boolean reportOnly) {

    public static PinPolicy of(String... pins) {
        return new PinPolicy(Set.of(pins), false);
    }

    public PinPolicy inReportOnlyMode() {
        return new PinPolicy(pins, true);
    }
}
