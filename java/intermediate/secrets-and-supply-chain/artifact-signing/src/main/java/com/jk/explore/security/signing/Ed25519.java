package com.jk.explore.security.signing;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.NamedParameterSpec;

/**
 * Ed25519 with the JDK. Keys are made from a fixed 32-byte seed so the demo is the same on every run, and Ed25519 signatures are
 * deterministic: the same key and message always give the same signature. Real signing keys come from a secure random source and stay
 * in protected hardware.
 */
final class Ed25519 {

    private Ed25519() {
    }

    static KeyPair fromSeed(byte[] seed) {
        if (seed.length != 32) {
            throw new IllegalArgumentException("an Ed25519 seed is 32 bytes");
        }
        SecureRandom fixed = new SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                System.arraycopy(seed, 0, bytes, 0, bytes.length);
            }
        };
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            generator.initialize(NamedParameterSpec.ED25519, fixed);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
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
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(key);
            signature.update(message);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** The 32-byte public key as RFC 8032 writes it: y in little-endian order, with the sign of x in the top bit. */
    static byte[] raw(PublicKey key) {
        EdECPoint point = ((EdECPublicKey) key).getPoint();
        byte[] bigEndian = point.getY().toByteArray();
        byte[] out = new byte[32];
        for (int i = 0; i < 32 && i < bigEndian.length; i++) {
            out[i] = bigEndian[bigEndian.length - 1 - i];
        }
        if (point.isXOdd()) {
            out[31] |= (byte) 0x80;
        }
        return out;
    }

}
