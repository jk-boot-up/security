package com.jk.explore.security.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

/**
 * What the bank knows about a client before any request arrives: its id, the exact redirect URIs it registered, the scopes it may ask for,
 * and, for a confidential client that runs on a server, the SHA-256 of its secret. A public client, such as an app on a phone, has no secret.
 */
record ClientRegistration(String clientId, List<String> redirectUris, Set<String> allowedScopes, byte[] secretHash) {

    static ClientRegistration publicClient(String clientId, List<String> redirectUris, Set<String> allowedScopes) {
        return new ClientRegistration(clientId, List.copyOf(redirectUris), Set.copyOf(allowedScopes), null);
    }

    static ClientRegistration confidentialClient(String clientId, List<String> redirectUris, Set<String> allowedScopes, String secret) {
        return new ClientRegistration(clientId, List.copyOf(redirectUris), Set.copyOf(allowedScopes), sha256(secret));
    }

    boolean confidential() {
        return secretHash != null;
    }

    /** Exact string comparison, as RFC 9700 section 2.1 requires: no prefixes, no wildcards, no normalisation. */
    boolean registered(String redirectUri) {
        return redirectUri != null && redirectUris.contains(redirectUri);
    }

    /** Compares hashes in constant time, so the time taken says nothing about how much of the secret was right. */
    boolean secretMatches(String secret) {
        return secret != null && MessageDigest.isEqual(secretHash, sha256(secret));
    }

    static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
