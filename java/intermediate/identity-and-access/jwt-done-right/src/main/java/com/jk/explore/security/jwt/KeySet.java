package com.jk.explore.security.jwt;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The public keys the bank's API trusts, by key id: the API's own copy of the authorization server's JWKS. A token can only
 * choose among these keys. An unknown key id is refused; nothing is fetched because a token asked for it.
 */
final class KeySet {

    private final Map<String, PublicKey> keys = new TreeMap<>();

    KeySet add(String kid, PublicKey key) {
        keys.put(kid, key);
        return this;
    }

    void remove(String kid) {
        keys.remove(kid);
    }

    Optional<PublicKey> find(String kid) {
        return Optional.ofNullable(keys.get(kid));
    }

    List<String> ids() {
        return List.copyOf(keys.keySet());
    }
}
