package com.jk.explore.security.passkeys;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * The bank's side of WebAuthn: it hands out single-use challenges, stores each customer's public keys, and checks every answer in a
 * fixed order: challenge, type, origin, relying party id hash, flags, credential, signature and counter. It stores nothing that could be
 * used to sign in.
 */
final class RelyingParty {

    record Options(String rpId, String challenge, List<byte[]> allowed) {
    }

    record Verdict(boolean accepted, String reason) {

        static Verdict yes(String reason) {
            return new Verdict(true, reason);
        }

        static Verdict no(String reason) {
            return new Verdict(false, reason);
        }

        String text() {
            return accepted ? "accepted (" + reason + ")" : "refused (" + reason + ")";
        }
    }

    /** What the bank keeps for each passkey: no private key, no shared secret. */
    static final class StoredCredential {
        final String user;
        final byte[] id;
        final byte[] publicKey;
        final String label;
        long signCount;
        boolean suspended;

        StoredCredential(String user, byte[] id, byte[] publicKey, String label) {
            this.user = user;
            this.id = id;
            this.publicKey = publicKey;
            this.label = label;
        }
    }

    private record Pending(String user, String type, boolean requireVerification, long expiresAt) {
    }

    static final long CHALLENGE_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String rpId;
    private final String origin;
    private final SimClock clock;
    private final Map<String, Pending> pending = new HashMap<>();
    private final List<StoredCredential> credentials = new ArrayList<>();
    private final Map<String, byte[]> passwordHashes = new HashMap<>();
    private final Map<String, byte[]> salts = new HashMap<>();
    private final Map<String, Set<String>> recoveryCodeHashes = new HashMap<>();

    RelyingParty(String rpId, String origin, SimClock clock) {
        this.rpId = rpId;
        this.origin = origin;
        this.clock = clock;
    }

    String rpId() {
        return rpId;
    }

    private String newChallenge(String user, String type, boolean requireVerification) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String challenge = B64.encode(bytes);
        pending.put(challenge, new Pending(user, type, requireVerification, clock.now() + CHALLENGE_SECONDS));
        return challenge;
    }

    Options startRegistration(String user) {
        return new Options(rpId, newChallenge(user, ClientData.CREATE, true), List.of());
    }

    /** Starts a sign-in. A payment asks for user verification (a PIN or biometric), not only a touch. */
    Options startSignIn(String user, boolean forPayment) {
        List<byte[]> allowed = credentials.stream().filter(c -> c.user.equals(user)).map(c -> c.id.clone()).toList();
        return new Options(rpId, newChallenge(user, ClientData.GET, forPayment), allowed);
    }

    private Verdict checkCommon(String user, byte[] clientDataJson, byte[] authenticatorData, String wantedType) {
        ClientData clientData;
        try {
            clientData = ClientData.parse(clientDataJson);
        } catch (IllegalArgumentException e) {
            return Verdict.no("client data unreadable: " + e.getMessage());
        }
        Pending waiting = pending.remove(clientData.challenge());
        if (waiting == null) {
            return Verdict.no("unknown or already used challenge");
        }
        if (!waiting.user().equals(user) || !waiting.type().equals(wantedType)) {
            return Verdict.no("challenge was issued for something else");
        }
        if (clock.now() > waiting.expiresAt()) {
            return Verdict.no("challenge expired");
        }
        if (!clientData.type().equals(wantedType)) {
            return Verdict.no("type is " + clientData.type() + ", not " + wantedType);
        }
        if (!clientData.origin().equals(origin)) {
            return Verdict.no("origin " + clientData.origin() + " is not " + origin);
        }
        AuthenticatorData data;
        try {
            data = AuthenticatorData.decode(authenticatorData);
        } catch (IllegalArgumentException e) {
            return Verdict.no("authenticator data unreadable: " + e.getMessage());
        }
        if (!MessageDigest.isEqual(data.rpIdHash(), Sha256.of(rpId))) {
            return Verdict.no("signed for another relying party id");
        }
        if (!data.has(AuthenticatorData.USER_PRESENT)) {
            return Verdict.no("no user presence");
        }
        if (waiting.requireVerification() && !data.has(AuthenticatorData.USER_VERIFIED)) {
            return Verdict.no("user verification required");
        }
        return null;
    }

    Verdict finishRegistration(String user, Authenticator.Registration registration, String label) {
        Verdict problem = checkCommon(user, registration.clientDataJson(), registration.authenticatorData(), ClientData.CREATE);
        if (problem != null) {
            return problem;
        }
        AuthenticatorData data = AuthenticatorData.decode(registration.authenticatorData());
        if (data.credentialId() == null || !MessageDigest.isEqual(data.credentialId(), registration.credentialId())) {
            return Verdict.no("credential id missing from the authenticator data");
        }
        if (credentials.stream().anyMatch(c -> MessageDigest.isEqual(c.id, registration.credentialId()))) {
            return Verdict.no("credential already registered");
        }
        try {
            publicKey(registration.publicKey());
        } catch (GeneralSecurityException e) {
            return Verdict.no("public key unreadable");
        }
        credentials.add(new StoredCredential(user, registration.credentialId().clone(), registration.publicKey().clone(), label));
        return Verdict.yes("passkey " + label + " registered");
    }

    Verdict finishSignIn(String user, Authenticator.Assertion assertion) {
        Verdict problem = checkCommon(user, assertion.clientDataJson(), assertion.authenticatorData(), ClientData.GET);
        if (problem != null) {
            return problem;
        }
        StoredCredential credential = credentials.stream()
                .filter(c -> c.user.equals(user) && MessageDigest.isEqual(c.id, assertion.credentialId()))
                .findFirst().orElse(null);
        if (credential == null) {
            return Verdict.no("no such passkey for " + user);
        }
        if (credential.suspended) {
            return Verdict.no("passkey suspended after a counter went backwards");
        }
        if (!verify(credential.publicKey, assertion)) {
            return Verdict.no("signature does not match");
        }
        long count = AuthenticatorData.decode(assertion.authenticatorData()).signCount();
        if ((count != 0 || credential.signCount != 0) && count <= credential.signCount) {
            credential.suspended = true;
            return Verdict.no("counter " + count + " is not above " + credential.signCount + ": possible cloned authenticator");
        }
        credential.signCount = count;
        return Verdict.yes("signed in as " + user + " with " + credential.label);
    }

    private static PublicKey publicKey(byte[] encoded) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static boolean verify(byte[] encodedKey, Authenticator.Assertion assertion) {
        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey(encodedKey));
            verifier.update(assertion.authenticatorData());
            verifier.update(Sha256.of(assertion.clientDataJson()));
            return verifier.verify(assertion.signature());
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    List<StoredCredential> credentialsOf(String user) {
        return credentials.stream().filter(c -> c.user.equals(user)).toList();
    }

    int pendingChallenges() {
        return pending.size();
    }

    // Passwords, recovery codes and the move to passkeys only.

    void setPassword(String user, char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        salts.put(user, salt);
        passwordHashes.put(user, pbkdf2(password, salt));
    }

    void disablePassword(String user) {
        passwordHashes.remove(user);
        salts.remove(user);
    }

    Verdict passwordSignIn(String user, char[] password) {
        byte[] stored = passwordHashes.get(user);
        if (stored == null) {
            return Verdict.no("password sign-in is switched off for " + user);
        }
        return MessageDigest.isEqual(stored, pbkdf2(password, salts.get(user)))
                ? Verdict.yes("signed in as " + user + " with a password")
                : Verdict.no("wrong username or password");
    }

    /** A one-time recovery code of 20 random base32-style characters; the bank keeps only its hash. */
    String issueRecoveryCode(String user) {
        String alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        recoveryCodeHashes.computeIfAbsent(user, u -> new HashSet<>()).add(B64.encode(Sha256.of(code.toString())));
        return code.toString();
    }

    Verdict recover(String user, String code) {
        Set<String> hashes = recoveryCodeHashes.get(user);
        String hash = B64.encode(Sha256.of(code.getBytes(StandardCharsets.UTF_8)));
        if (hashes == null || !hashes.remove(hash)) {
            return Verdict.no("recovery code unknown or already used");
        }
        return Verdict.yes("recovered " + user + "; register a new passkey now");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password, salt, 100_000, 256)).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
