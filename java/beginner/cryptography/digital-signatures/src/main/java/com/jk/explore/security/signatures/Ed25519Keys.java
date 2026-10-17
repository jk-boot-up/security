package com.jk.explore.security.signatures;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;

/**
 * Ed25519 keys. A new pair is made with {@link #newKeyPair()}. For the demo and the tests, a key can also be built from its raw
 * bytes: a 32-byte private seed, and a 32-byte public key, which is how the standard (RFC 8032) writes its test vectors.
 */
public final class Ed25519Keys {

    private Ed25519Keys() {
    }

    public static KeyPair newKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static PrivateKey privateFromSeed(byte[] seed) {
        try {
            return KeyFactory.getInstance("Ed25519").generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, seed));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A public key from its 32 raw bytes: the y coordinate in little-endian order, with the sign of x in the top bit. */
    public static PublicKey publicFromRaw(byte[] raw) {
        byte[] littleEndian = raw.clone();
        boolean xOdd = (littleEndian[31] & 0x80) != 0;
        littleEndian[31] &= 0x7f;
        byte[] bigEndian = new byte[32];
        for (int i = 0; i < 32; i++) {
            bigEndian[i] = littleEndian[31 - i];
        }
        try {
            return KeyFactory.getInstance("Ed25519")
                    .generatePublic(new EdECPublicKeySpec(NamedParameterSpec.ED25519, new EdECPoint(xOdd, new BigInteger(1, bigEndian))));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
