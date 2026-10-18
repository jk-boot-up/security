package com.jk.explore.security.passkeys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256, used for the relying party id hash and for the hash of the client data. */
final class Sha256 {

    private Sha256() {
    }

    static byte[] of(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    static byte[] of(String text) {
        return of(text.getBytes(StandardCharsets.UTF_8));
    }
}
