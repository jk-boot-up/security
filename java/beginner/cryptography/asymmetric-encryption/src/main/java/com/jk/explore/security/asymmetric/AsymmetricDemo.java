package com.jk.explore.security.asymmetric;

import java.security.KeyPair;
import java.util.Arrays;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
public class AsymmetricDemo {

    static final byte[] STATEMENT_KEY = Hex.bytes("statement-key-32-bytes-long-demo");
    static final byte[] ACCOUNT = Hex.bytes("account=ANA-1");

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys are random; only facts that do not depend on them are printed. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1SharingProblem(out);
        act2RsaOaep(out);
        act3Hybrid(out);
        act4KeyAgreement(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1SharingProblem(StringBuilder out) {
        line(out, "ONE. The problem: sharing a key.");
        Wire wire = new Wire();
        byte[] key = STATEMENT_KEY;
        wire.send(key);
        line(out, "  the bank wants to send Ana an encrypted statement, and sends the key over the open network first");
        line(out, "  Eve sees every message on the wire, including the key: " + Arrays.equals(wire.seenByEve().get(0), key)
                + "  <- EVE CAN OPEN ANYTHING NOW");
        line(out, "  a secret key cannot be sent in the clear, and the bank and Ana have never met");
    }

    private static void act2RsaOaep(StringBuilder out) {
        line(out, "TWO. A key pair: lock with the public key, open with the private key.");
        KeyPair ana = RsaBox.newKeyPair();
        KeyPair eve = RsaBox.newKeyPair();
        Wire wire = new Wire();
        byte[] locked = wire.send(RsaBox.lock(ana.getPublic(), STATEMENT_KEY));
        line(out, "  Ana publishes her public key, " + ana.getPublic().getEncoded().length + " bytes, and keeps the private key");
        line(out, "  the bank locks the " + STATEMENT_KEY.length + "-byte key with it: " + locked.length + " bytes on the wire");
        line(out, "  Ana opens it: " + Arrays.equals(RsaBox.unlock(ana.getPrivate(), locked), STATEMENT_KEY));
        line(out, "  Eve has the public key and the locked message, and tries her own private key: " + tryUnlock(eve, locked));
        byte[] again = RsaBox.lock(ana.getPublic(), STATEMENT_KEY);
        line(out, "  locking the same key again gives the same bytes: " + Arrays.equals(locked, again) + "  <- OAEP ADDS RANDOMNESS");
        line(out, "  at most " + RsaBox.maxMessageBytes() + " bytes fit: " + tooLong(ana));
    }

    private static void act3Hybrid(StringBuilder out) {
        line(out, "THREE. Hybrid encryption: RSA for the key, AES for the data.");
        KeyPair ana = RsaBox.newKeyPair();
        byte[] statement = new byte[10_000];
        Arrays.fill(statement, (byte) 'x');
        Envelope envelope = HybridBox.seal(ana.getPublic(), ACCOUNT, statement);
        line(out, "  a " + statement.length + "-byte statement is sealed with a fresh AES key, and only that key is locked with RSA");
        line(out, "  the envelope: " + envelope.lockedKey().length + " bytes of locked key, " + envelope.nonce().length
                + " of nonce, " + envelope.ciphertextWithTag().length + " of cipher text and tag: " + envelope.totalLength() + " in all");
        line(out, "  Ana opens it: " + Arrays.equals(HybridBox.open(ana.getPrivate(), ACCOUNT, envelope), statement));
        KeyPair eve = RsaBox.newKeyPair();
        line(out, "  Eve tries her own private key: " + refused(() -> HybridBox.open(eve.getPrivate(), ACCOUNT, envelope)));
        byte[] flipped = envelope.ciphertextWithTag().clone();
        flipped[0] ^= 1;
        line(out, "  one bit of the cipher text is flipped: "
                + refused(() -> HybridBox.open(ana.getPrivate(), ACCOUNT, new Envelope(envelope.lockedKey(), envelope.nonce(), flipped))));
    }

    private static void act4KeyAgreement(StringBuilder out) {
        line(out, "FOUR. Key agreement: no key is sent at all.");
        KeyPair bank = KeyAgreementBox.newKeyPair();
        KeyPair ana = KeyAgreementBox.newKeyPair();
        Wire wire = new Wire();
        wire.send(bank.getPublic().getEncoded());
        wire.send(ana.getPublic().getEncoded());
        byte[] salt = Hex.bytes("session-1");
        byte[] bankKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(bank.getPrivate(), ana.getPublic()), salt, ACCOUNT);
        byte[] anaKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(ana.getPrivate(), bank.getPublic()), salt, ACCOUNT);
        line(out, "  the bank and Ana swap public keys, " + bank.getPublic().getEncoded().length + " bytes each, and nothing else");
        line(out, "  each combines its own private key with the other's public key and gets a " + bankKey.length + "-byte key");
        line(out, "  the two keys are the same: " + Arrays.equals(bankKey, anaKey));
        KeyPair eve = KeyAgreementBox.newKeyPair();
        byte[] eveKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(eve.getPrivate(), ana.getPublic()), salt, ACCOUNT);
        line(out, "  Eve saw both public keys and makes her own: her key is the same too: " + Arrays.equals(eveKey, bankKey));
        KeyPair bank2 = KeyAgreementBox.newKeyPair();
        byte[] nextKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(bank2.getPrivate(), ana.getPublic()), salt, ACCOUNT);
        line(out, "  a new session with a new bank key gives a different key: " + !Arrays.equals(nextKey, bankKey)
                + "  <- AN OLD SESSION'S KEY IS NOT IN THE NEW ONE");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: whose key is it?");
        KeyPair ana = RsaBox.newKeyPair();
        KeyPair mallory = RsaBox.newKeyPair();
        // Mallory sits on the wire and swaps Ana's public key for her own, and later passes the message on to Ana.
        Envelope toMallory = HybridBox.seal(mallory.getPublic(), ACCOUNT, Hex.bytes("PIN is 4821"));
        byte[] read = HybridBox.open(mallory.getPrivate(), ACCOUNT, toMallory);
        Envelope onward = HybridBox.seal(ana.getPublic(), ACCOUNT, read);
        line(out, "  Mallory swaps Ana's public key for her own, and the bank locks a message to the wrong key");
        line(out, "  Mallory reads it: " + Hex.text(read) + ", locks it again for Ana, and sends it on");
        line(out, "  Ana opens what she receives: " + Hex.text(HybridBox.open(ana.getPrivate(), ACCOUNT, onward))
                + "  <- NOBODY NOTICED");
        line(out, "  nothing in the maths says whose public key it is: signatures and certificates are the answer");
        KeyPair x = KeyAgreementBox.newKeyPair();
        line(out, "  sizes: an RSA-2048 public key is " + ana.getPublic().getEncoded().length + " bytes, an X25519 one is "
                + x.getPublic().getEncoded().length);
        line(out, "  and a stolen private key opens every message ever locked to its public key");
    }

    private static String tryUnlock(KeyPair keys, byte[] locked) {
        try {
            RsaBox.unlock(keys.getPrivate(), locked);
            return "opened";
        } catch (RsaBox.NotForThisKeyException e) {
            return "refused";
        }
    }

    private static String tooLong(KeyPair keys) {
        try {
            RsaBox.lock(keys.getPublic(), new byte[RsaBox.maxMessageBytes() + 1]);
            return "a longer message fits";
        } catch (IllegalArgumentException e) {
            return "one byte more is refused";
        }
    }

    private static String refused(Runnable action) {
        try {
            action.run();
            return "opened";
        } catch (RsaBox.NotForThisKeyException e) {
            return "refused";
        }
    }
}
