package com.jk.explore.security.secrets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The payment provider's side of an API key. During a rotation two versions are accepted, so the store can switch to the
 * new key without a moment in which neither works; then the old version is revoked.
 */
final class PaymentProvider {

    private final Map<String, Secret> keys = new LinkedHashMap<>();

    void issue(String version, Secret key) {
        keys.put(version, key);
    }

    void revoke(String version) {
        keys.remove(version);
    }

    boolean accepts(Secret presented) {
        boolean accepted = false;
        for (Secret key : keys.values()) {
            accepted |= key.matches(presented);
        }
        return accepted;
    }

    List<String> activeVersions() {
        return List.copyOf(keys.keySet());
    }
}
