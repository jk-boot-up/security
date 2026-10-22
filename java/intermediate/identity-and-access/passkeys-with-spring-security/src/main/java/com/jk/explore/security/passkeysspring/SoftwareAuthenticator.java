package com.jk.explore.security.passkeysspring;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;

/**
 * A phone's authenticator in software. It makes a P-256 key pair per relying party and keeps the private key, signs what the relying party asks it to sign, counts its
 * signatures, and reports whether the user touched it and was verified (fingerprint or PIN). It builds the same bytes that a real authenticator and browser would.
 */
final class SoftwareAuthenticator {

    /** What the page's script would send to the relying party after {@code navigator.credentials.create}. */
    record Registration(String credentialId, String attestationObject, String clientDataJson) {
    }

    /** What the page's script would send after {@code navigator.credentials.get}. */
    record Assertion(String credentialId, String authenticatorData, String clientDataJson, String signature, String userHandle) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeyPair pair;
    private final byte[] credentialId = new byte[16];
    private long counter;
    private boolean backupEligible;
    private boolean backupState;
    private String registeredRpId;
    private String userHandle;

    SoftwareAuthenticator() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            this.pair = generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        RANDOM.nextBytes(credentialId);
    }

    /** An authenticator with no passkey at all, such as a new laptop. */
    static SoftwareAuthenticator empty() {
        return new SoftwareAuthenticator();
    }

    /** An attacker's key pair under the credential id of this one, which is all that a copy of the bank's records can give. */
    SoftwareAuthenticator impostor() {
        SoftwareAuthenticator other = new SoftwareAuthenticator();
        System.arraycopy(credentialId, 0, other.credentialId, 0, credentialId.length);
        other.registeredRpId = registeredRpId;
        other.userHandle = userHandle;
        return other;
    }

    /** A copy of this authenticator, with the same key and credential id, as an attacker who got hold of the key would make. */
    SoftwareAuthenticator cloneIt() {
        SoftwareAuthenticator copy = new SoftwareAuthenticator(pair, credentialId.clone());
        copy.counter = counter;
        copy.backupEligible = backupEligible;
        copy.backupState = backupState;
        copy.registeredRpId = registeredRpId;
        copy.userHandle = userHandle;
        return copy;
    }

    private SoftwareAuthenticator(KeyPair pair, byte[] credentialId) {
        this.pair = pair;
        System.arraycopy(credentialId, 0, this.credentialId, 0, credentialId.length);
    }

    /** A passkey that syncs through an account: eligible for backup and backed up, and with a counter that stays 0. */
    SoftwareAuthenticator synced() {
        this.backupEligible = true;
        this.backupState = true;
        return this;
    }

    String credentialId() {
        return B64.encode(credentialId);
    }

    long counter() {
        return counter;
    }

    void setCounter(long value) {
        this.counter = value;
    }

    int publicKeyLength() {
        return pair.getPublic().getEncoded().length;
    }

    /** True if this authenticator holds a passkey made for exactly this relying party id. */
    boolean hasPasskeyFor(String rpId) {
        return rpId.equals(registeredRpId);
    }

    String userHandle() {
        return userHandle;
    }

    Registration create(String rpId, String origin, String challenge, boolean userVerified, String forUserHandle) {
        this.registeredRpId = rpId;
        this.userHandle = forUserHandle;
        byte[] clientData = clientData("webauthn.create", challenge, origin);
        byte[] authData = authenticatorData(rpId, userVerified, true);
        byte[] attestation = Cbor.map("fmt", "none", "attStmt", Map.of(), "authData", authData);
        return new Registration(credentialId(), B64.encode(attestation), B64.encode(clientData));
    }

    /** Signs a sign-in. {@code asRpId} is the relying party id the authenticator hashes in, which is the one the page asked for. */
    Assertion sign(String asRpId, String origin, String challenge, boolean userVerified, boolean countUp) {
        if (countUp && !backupEligible) {
            counter++;
        }
        byte[] clientData = clientData("webauthn.get", challenge, origin);
        byte[] authData = authenticatorData(asRpId, userVerified, false);
        return new Assertion(credentialId(), B64.encode(authData), B64.encode(clientData), B64.encode(signature(authData, clientData)), this.userHandle);
    }

    byte[] signature(byte[] authData, byte[] clientData) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(pair.getPrivate());
            signature.update(authData);
            signature.update(MessageDigest.getInstance("SHA-256").digest(clientData));
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] clientData(String type, String challenge, String origin) {
        return ("{\"type\":\"" + type + "\",\"challenge\":\"" + challenge + "\",\"origin\":\"" + origin + "\",\"crossOrigin\":false}").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] authenticatorData(String rpId, boolean userVerified, boolean withCredential) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(MessageDigest.getInstance("SHA-256").digest(rpId.getBytes(StandardCharsets.UTF_8)));
            int flags = 0x01 | (userVerified ? 0x04 : 0) | (backupEligible ? 0x08 : 0) | (backupState ? 0x10 : 0) | (withCredential ? 0x40 : 0);
            out.write(flags);
            out.writeBytes(ByteBuffer.allocate(4).putInt((int) counter).array());
            if (withCredential) {
                out.writeBytes(new byte[16]);
                out.writeBytes(ByteBuffer.allocate(2).putShort((short) credentialId.length).array());
                out.writeBytes(credentialId);
                ECPublicKey key = (ECPublicKey) pair.getPublic();
                out.writeBytes(Cbor.map(1, 2, 3, -7, -1, 1, -2, fixed(key.getW().getAffineX().toByteArray()), -3, fixed(key.getW().getAffineY().toByteArray())));
            }
            return out.toByteArray();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] fixed(byte[] value) {
        byte[] out = new byte[32];
        int length = Math.min(value.length, 32);
        System.arraycopy(value, value.length - length, out, 32 - length, length);
        return out;
    }

}
