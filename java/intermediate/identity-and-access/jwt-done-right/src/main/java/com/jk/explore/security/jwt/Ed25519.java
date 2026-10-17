package com.jk.explore.security.jwt;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

/**
 * Ed25519 keys and signatures with the JDK alone (RFC 8032, used in JOSE as "EdDSA", RFC 8037). Ed25519 signatures are
 * deterministic: the same key and message always give the same 64 bytes, which is why the demo can print exact tokens.
 */
final class Ed25519 {

    private Ed25519() {
    }

    /** A key pair from a fixed 32-byte seed (the RFC 8032 private key). The JDK derives the public key from it. */
    static KeyPair fromSeed(byte[] seed) {
        if (seed.length != 32) {
            throw new IllegalArgumentException("an Ed25519 seed is 32 bytes");
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            generator.initialize(NamedParameterSpec.ED25519, new FixedBytes(seed));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ed25519 is part of every Java 25 runtime", e);
        }
    }

    static PrivateKey privateKey(byte[] seed) {
        try {
            return KeyFactory.getInstance("Ed25519").generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, seed));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("not an Ed25519 private key", e);
        }
    }

    /** A public key from its 32-byte encoding: y in little-endian order, with the top bit saying whether x is odd (RFC 8032 section 5.1.2). */
    static PublicKey publicKey(byte[] raw) {
        if (raw.length != 32) {
            throw new IllegalArgumentException("an Ed25519 public key is 32 bytes");
        }
        byte[] bigEndian = new byte[32];
        for (int i = 0; i < 32; i++) {
            bigEndian[i] = raw[31 - i];
        }
        boolean xOdd = (bigEndian[0] & 0x80) != 0;
        bigEndian[0] &= 0x7f;
        try {
            EdECPoint point = new EdECPoint(xOdd, new BigInteger(1, bigEndian));
            return KeyFactory.getInstance("Ed25519").generatePublic(new EdECPublicKeySpec(NamedParameterSpec.ED25519, point));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("not an Ed25519 public key", e);
        }
    }

    /** The 32-byte encoding of a public key, the inverse of {@link #publicKey(byte[])}; it is what a JWK's "x" member holds. */
    static byte[] encode(PublicKey key) {
        EdECPoint point = ((EdECPublicKey) key).getPoint();
        byte[] y = point.getY().toByteArray();
        byte[] bigEndian = new byte[32];
        int copy = Math.min(y.length, 32);
        System.arraycopy(y, y.length - copy, bigEndian, 32 - copy, copy);
        if (point.isXOdd()) {
            bigEndian[0] |= (byte) 0x80;
        }
        byte[] raw = new byte[32];
        for (int i = 0; i < 32; i++) {
            raw[i] = bigEndian[31 - i];
        }
        return raw;
    }

    static byte[] sign(PrivateKey key, byte[] message) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update(message);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean verify(PublicKey key, byte[] message, byte[] signatureBytes) {
        if (signatureBytes.length != 64) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(key);
            signature.update(message);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** Hands the key generator a fixed seed instead of random bytes, so a demo key is the same on every run. Never use outside a demo. */
    private static final class FixedBytes extends SecureRandom {

        private final byte[] seed;

        FixedBytes(byte[] seed) {
            this.seed = seed.clone();
        }

        @Override
        public void nextBytes(byte[] bytes) {
            Arrays.fill(bytes, (byte) 0);
            System.arraycopy(seed, 0, bytes, 0, Math.min(seed.length, bytes.length));
        }
    }
}
