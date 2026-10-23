package com.jk.explore.security.cryptoagility;

import java.util.Map;

/** The bank's list of algorithms: the transformations and digests that code may name, and what to do about each. */
public final class Policy {

    private static final Map<String, Status> LIST = Map.ofEntries(
            Map.entry("AES/GCM/NoPadding", Status.APPROVED),
            Map.entry("ChaCha20-Poly1305", Status.APPROVED),
            Map.entry("SHA-256", Status.APPROVED),
            Map.entry("HmacSHA256", Status.APPROVED),
            Map.entry("SHA256withRSA", Status.APPROVED),
            Map.entry("PBKDF2WithHmacSHA256", Status.APPROVED),
            Map.entry("AES/CBC/PKCS5Padding", Status.DEPRECATED),
            Map.entry("RSA/ECB/PKCS1Padding", Status.DEPRECATED),
            Map.entry("SHA-1", Status.DEPRECATED),
            Map.entry("SHA1PRNG", Status.DEPRECATED),
            Map.entry("AES/ECB/PKCS5Padding", Status.FORBIDDEN),
            Map.entry("MD5", Status.FORBIDDEN),
            Map.entry("DESede/CBC/PKCS5Padding", Status.FORBIDDEN));

    private Policy() {
    }

    public static Status of(String algorithm) {
        return LIST.getOrDefault(algorithm, Status.UNKNOWN);
    }
}
