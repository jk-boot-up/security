package com.jk.explore.security.pqc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import javax.crypto.KDF;
import javax.crypto.KEM;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;

/**
 * A hybrid key exchange: X25519 and ML-KEM-768 side by side, the two secrets joined by HKDF with the whole transcript as context.
 * The client sends its X25519 public key and its ML-KEM public key; the server answers with its own X25519 public key and an ML-KEM ciphertext.
 */
final class Hybrid {

    /** Everything that crosses the network, and what each side kept. */
    record Exchange(KeyPair clientX, KeyPair clientK, KeyPair serverX, byte[] ciphertext, byte[] clientKey, byte[] serverKey,
            byte[] classicalSecret, byte[] kemSecret) {
    }

    private Hybrid() {
    }

    static Exchange run() throws GeneralSecurityException {
        KeyPair clientX = KeyPairGenerator.getInstance("X25519").generateKeyPair();
        KeyPair clientK = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        KeyPair serverX = KeyPairGenerator.getInstance("X25519").generateKeyPair();

        // Server side: one X25519 agreement, one encapsulation, one derivation.
        byte[] serverClassical = agree(serverX, clientX);
        KEM.Encapsulated encapsulated = KEM.getInstance("ML-KEM").newEncapsulator(clientK.getPublic()).encapsulate();
        byte[] ciphertext = encapsulated.encapsulation();
        byte[] serverKey = combine(serverClassical, encapsulated.key().getEncoded(), clientX, clientK, serverX, ciphertext);

        // Client side: the same agreement from the other end, one decapsulation, the same derivation.
        byte[] clientClassical = agree(clientX, serverX);
        byte[] clientKem = KEM.getInstance("ML-KEM").newDecapsulator(clientK.getPrivate()).decapsulate(ciphertext).getEncoded();
        byte[] clientKey = combine(clientClassical, clientKem, clientX, clientK, serverX, ciphertext);
        return new Exchange(clientX, clientK, serverX, ciphertext, clientKey, serverKey, clientClassical, clientKem);
    }

    /** The X25519 secret of one side's private key and the other side's public key. */
    static byte[] agree(KeyPair own, KeyPair peer) throws GeneralSecurityException {
        KeyAgreement agreement = KeyAgreement.getInstance("X25519");
        agreement.init(own.getPrivate());
        agreement.doPhase(peer.getPublic(), true);
        return agreement.generateSecret();
    }

    /** HKDF-SHA256 over both secrets; the transcript (all public values) is the context, so a changed message gives a different key. */
    static byte[] combine(byte[] classical, byte[] kem, KeyPair clientX, KeyPair clientK, KeyPair serverX, byte[] ciphertext)
            throws GeneralSecurityException {
        ByteArrayOutputStream info = new ByteArrayOutputStream();
        info.writeBytes("bank hybrid key exchange v1".getBytes(StandardCharsets.US_ASCII));
        info.writeBytes(clientX.getPublic().getEncoded());
        info.writeBytes(clientK.getPublic().getEncoded());
        info.writeBytes(serverX.getPublic().getEncoded());
        info.writeBytes(ciphertext);
        return derive(classical, kem, info.toByteArray());
    }

    static byte[] derive(byte[] classical, byte[] kem, byte[] info) throws GeneralSecurityException {
        ByteArrayOutputStream secrets = new ByteArrayOutputStream();
        secrets.writeBytes(classical);
        secrets.writeBytes(kem);
        HKDFParameterSpec spec = HKDFParameterSpec.ofExtract().addIKM(secrets.toByteArray()).thenExpand(info, 32);
        SecretKey key = KDF.getInstance("HKDF-SHA256").deriveKey("AES", spec);
        return key.getEncoded();
    }

    static boolean same(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
