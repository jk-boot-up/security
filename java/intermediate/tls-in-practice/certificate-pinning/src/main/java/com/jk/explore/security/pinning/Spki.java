package com.jk.explore.security.pinning;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

/** The pin of a key: the SHA-256 hash of its SubjectPublicKeyInfo, in Base64 (the form used by HPKP, OkHttp and Android's network security configuration). */
public final class Spki {

    private Spki() {
    }

    public static String pin(PublicKey key) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String pin(X509Certificate cert) {
        return pin(cert.getPublicKey());
    }
}
