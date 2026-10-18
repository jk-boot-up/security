package com.jk.explore.security.passkeys;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A model authenticator: a phone, a laptop or a security key. It makes one ES256 (P-256) key pair per relying party, keeps the private
 * key to itself, and signs only for the relying party id the key was made for. A device-bound authenticator counts its signatures; a
 * synced passkey, shared between a person's devices, reports a counter of zero.
 */
final class Authenticator {

    /** What registration returns: the new credential's id and public key, inside authenticator data with the attested-credential flag. */
    record Registration(byte[] credentialId, byte[] publicKey, byte[] authenticatorData, byte[] clientDataJson) {
    }

    /** What signing in returns. The signature covers the authenticator data and the SHA-256 of the client data. */
    record Assertion(byte[] credentialId, byte[] authenticatorData, byte[] clientDataJson, byte[] signature) {

        Assertion withCredentialId(byte[] other) {
            return new Assertion(other, authenticatorData, clientDataJson, signature);
        }
    }

    private static final class Credential {
        final String rpId;
        final byte[] id;
        final KeyPair keys;
        long counter;

        Credential(String rpId, byte[] id, KeyPair keys, long counter) {
            this.rpId = rpId;
            this.id = id;
            this.keys = keys;
            this.counter = counter;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String name;
    private final boolean synced;
    private final List<Credential> credentials = new ArrayList<>();

    Authenticator(String name, boolean synced) {
        this.name = name;
        this.synced = synced;
    }

    String name() {
        return name;
    }

    int credentialCount() {
        return credentials.size();
    }

    /** Makes a new key pair for this relying party. The user has touched the device; userVerified says whether a PIN or biometric was used too. */
    Registration create(String rpId, byte[] clientDataJson, boolean userVerified) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            KeyPair keys = generator.generateKeyPair();
            byte[] id = new byte[16];
            RANDOM.nextBytes(id);
            credentials.add(new Credential(rpId, id, keys, 0));
            int flags = AuthenticatorData.USER_PRESENT | AuthenticatorData.ATTESTED_CREDENTIAL
                    | (userVerified ? AuthenticatorData.USER_VERIFIED : 0);
            byte[] data = new AuthenticatorData(Sha256.of(rpId), flags, 0, id).encode();
            return new Registration(id.clone(), keys.getPublic().getEncoded(), data, clientDataJson);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is part of every Java runtime", e);
        }
    }

    /**
     * Signs in to the relying party with a credential made for exactly that relying party id, if it has one and, when the server lists
     * credentials, one of those. With no such credential there is nothing to sign and nothing is returned.
     */
    Optional<Assertion> get(String rpId, List<byte[]> allowed, byte[] clientDataJson, boolean userVerified) {
        for (Credential credential : credentials) {
            if (!credential.rpId.equals(rpId)) {
                continue;
            }
            if (!allowed.isEmpty() && allowed.stream().noneMatch(a -> MessageDigest.isEqual(a, credential.id))) {
                continue;
            }
            if (!synced) {
                credential.counter++;
            }
            int flags = AuthenticatorData.USER_PRESENT | (userVerified ? AuthenticatorData.USER_VERIFIED : 0);
            byte[] data = new AuthenticatorData(Sha256.of(rpId), flags, credential.counter, null).encode();
            return Optional.of(new Assertion(credential.id.clone(), data, clientDataJson, sign(credential.keys, data, clientDataJson)));
        }
        return Optional.empty();
    }

    static byte[] sign(KeyPair keys, byte[] authenticatorData, byte[] clientDataJson) {
        try {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(keys.getPrivate(), RANDOM);
            signer.update(authenticatorData);
            signer.update(Sha256.of(clientDataJson));
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A copy of this authenticator's keys and counters at this moment, as an attacker who extracted the keys would hold. */
    Authenticator copyAs(String copyName) {
        Authenticator copy = new Authenticator(copyName, synced);
        for (Credential credential : credentials) {
            copy.credentials.add(new Credential(credential.rpId, credential.id.clone(), credential.keys, credential.counter));
        }
        return copy;
    }
}
