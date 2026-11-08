package com.jk.explore.security.asymmetric;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Arrays;
import javax.crypto.KDF;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.HKDFParameterSpec;

/**
 * Key agreement with X25519 (elliptic-curve Diffie-Hellman). Each side makes a key pair and sends only its public key. Each
 * combines its own private key with the other's public key and gets the same secret, which neither side ever sent. The secret
 * is then run through HKDF to make the key to use.
 */
public final class KeyAgreementBox {

    private KeyAgreementBox() {
    }

    public static KeyPair newKeyPair() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A private key from its 32 raw bytes, as written in RFC 7748. */
    public static PrivateKey privateKey(byte[] raw) {
        try {
            return KeyFactory.getInstance("X25519").generatePrivate(new XECPrivateKeySpec(NamedParameterSpec.X25519, raw));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A public key from its 32 raw bytes, as written in RFC 7748 (least significant byte first). */
    public static PublicKey publicKey(byte[] raw) {
        byte[] bigEndian = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            bigEndian[i] = raw[raw.length - 1 - i];
        }
        try {
            return KeyFactory.getInstance("X25519")
                    .generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, new BigInteger(1, bigEndian)));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** The 32 raw key bytes: the encoded key ends with them, after a short header naming X25519. */
    public static byte[] raw(Key key) {
        byte[] encoded = key.getEncoded();
        return Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
    }

    /** The raw shared secret from one side's private key and the other side's public key. */
    public static byte[] sharedSecret(PrivateKey mine, PublicKey theirs) {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance("X25519");
            agreement.init(mine);
            agreement.doPhase(theirs, true);
            return agreement.generateSecret();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A 32-byte key made from the shared secret with HKDF-SHA-256, tied to a purpose by {@code info}. */
    public static byte[] deriveKey(byte[] sharedSecret, byte[] salt, byte[] info) {
        return hkdf(sharedSecret, salt, info, 32);
    }

    /** HKDF (RFC 5869) with SHA-256: extract a key from the input, then expand it to {@code length} bytes. */
    public static byte[] hkdf(byte[] inputKeyMaterial, byte[] salt, byte[] info, int length) {
        try {
            KDF kdf = KDF.getInstance("HKDF-SHA256");
            return kdf.deriveData(HKDFParameterSpec.ofExtract().addIKM(inputKeyMaterial).addSalt(salt).thenExpand(info, length));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
