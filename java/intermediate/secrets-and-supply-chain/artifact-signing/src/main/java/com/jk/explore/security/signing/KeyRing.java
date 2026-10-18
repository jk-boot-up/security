package com.jk.explore.security.signing;

import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The public keys a deployment trusts, each pinned to one channel, with the days it is valid from and, once revoked, valid until. */
final class KeyRing {

    record TrustedKey(String keyId, String channel, PublicKey publicKey, int validFromDay, int revokedFromDay) {

        boolean validOn(int day) {
            return day >= validFromDay && day < revokedFromDay;
        }
    }

    static final int NEVER = Integer.MAX_VALUE;

    private final Map<String, TrustedKey> keys = new LinkedHashMap<>();

    KeyRing trust(SigningKey key, int validFromDay) {
        keys.put(key.keyId(), new TrustedKey(key.keyId(), key.channel(), key.publicKey(), validFromDay, NEVER));
        return this;
    }

    /** Signatures dated on or after this day no longer count. Signatures made before it stay valid. */
    void revoke(String keyId, int fromDay) {
        TrustedKey key = keys.get(keyId);
        keys.put(keyId, new TrustedKey(key.keyId(), key.channel(), key.publicKey(), key.validFromDay(), fromDay));
    }

    Optional<TrustedKey> find(String keyId) {
        return Optional.ofNullable(keys.get(keyId));
    }

    boolean isStoreKey(String keyId) {
        return keys.containsKey(keyId);
    }
}
