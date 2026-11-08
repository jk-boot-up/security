package com.jk.explore.security.asymmetric;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. RSA keys are random, so for them
 * only sizes, equalities and refusals are printed; the by-hand examples and the X25519 keys (from RFC 7748) are fixed.
 */
public class AsymmetricDemo {

    static final byte[] STATEMENT_KEY = Hex.bytes("statement-key-32-bytes-long-demo");
    static final byte[] ACCOUNT = Hex.bytes("account=ANA-1");
    // the X25519 test keys of RFC 7748: Alice is the bank, Bob is Ana
    static final String BANK_PRIVATE = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
    static final String BANK_PUBLIC = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    static final String ANA_PRIVATE = "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb";
    static final String ANA_PUBLIC = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1SharingProblem(out);
        act2KeyPair(out);
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
        wire.send(STATEMENT_KEY);
        byte[] seen = wire.seenByEve().get(0);
        line(out, "  WHAT THE BANK SENDS (the key, in the clear):");
        line(out, "    key   " + Hex.text(STATEMENT_KEY));
        line(out, "  WHAT EVE SEES ON THE WIRE:");
        line(out, "    key   " + Hex.text(seen));
        line(out, "  Eve's copy is the key: " + Arrays.equals(seen, STATEMENT_KEY) + "  <- EVE HAS IT");
        line(out, "  they have never met: no safe way to hand it over");
    }

    private static void act2KeyPair(StringBuilder out) {
        line(out, "TWO. A key pair: the public key locks, the private key opens.");
        ByHand.RsaKeys tiny = ByHand.rsaKeys(89, 97, 17);
        long pin = 4821;
        long locked = ByHand.lock(tiny, pin);
        long phi = (tiny.p() - 1) * (tiny.q() - 1);
        line(out, "  BY HAND, WITH TINY NUMBERS:");
        line(out, "    two primes     p = " + tiny.p() + ", q = " + tiny.q());
        line(out, "    public key     n = p x q = " + tiny.n() + ", e = " + tiny.e());
        line(out, "    private key    d = " + tiny.d() + ", as " + tiny.e() + " x " + tiny.d() + " = " + tiny.e() * tiny.d()
                + " = " + (tiny.p() - 1) + " x " + (tiny.q() - 1) + " + " + (tiny.e() * tiny.d() - phi));
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    message        m = " + pin);
        line(out, "  AFTER ENCRYPTION (with the public key):");
        line(out, "    locked         c = m^e mod n = " + locked);
        line(out, "  OPENING (with the private key):");
        line(out, "    opened         c^d mod n = " + ByHand.open(tiny, locked));
        line(out, "    locked again   " + ByHand.lock(tiny, pin) + ", the same  <- NO RANDOMNESS");

        KeyPair ana = RsaBox.newKeyPair();
        KeyPair eve = RsaBox.newKeyPair();
        byte[] lockedKey = new Wire().send(RsaBox.lock(ana.getPublic(), STATEMENT_KEY));
        line(out, "  REAL RSA-2048 WITH OAEP PADDING:");
        line(out, "    Ana's public key   " + ana.getPublic().getEncoded().length + " bytes, published");
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    the key            " + STATEMENT_KEY.length + " bytes");
        line(out, "  AFTER ENCRYPTION:");
        line(out, "    on the wire        " + lockedKey.length + " bytes");
        line(out, "  OPENING:");
        line(out, "    Ana's private key  opens it: " + Arrays.equals(RsaBox.unlock(ana.getPrivate(), lockedKey), STATEMENT_KEY));
        line(out, "    Eve's private key  " + tryUnlock(eve, lockedKey));
        byte[] again = RsaBox.lock(ana.getPublic(), STATEMENT_KEY);
        line(out, "    locked again, the same bytes: " + Arrays.equals(lockedKey, again) + "  <- OAEP ADDS RANDOMNESS");
        line(out, "  at most " + RsaBox.maxMessageBytes() + " bytes fit: " + tooLong(ana));
    }

    private static void act3Hybrid(StringBuilder out) {
        line(out, "THREE. Hybrid encryption: RSA for the key, AES for the data.");
        KeyPair ana = RsaBox.newKeyPair();
        byte[] statement = new byte[10_000];
        Arrays.fill(statement, (byte) 'x');
        Envelope envelope = HybridBox.seal(ana.getPublic(), ACCOUNT, statement);
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    statement      " + statement.length + " bytes, far too big for RSA");
        line(out, "    fresh AES key  32 bytes, made for this one message");
        line(out, "  AFTER ENCRYPTION (the envelope):");
        line(out, "    locked key     " + envelope.lockedKey().length + " bytes (the AES key, locked with RSA)");
        line(out, "    nonce          " + envelope.nonce().length + " bytes");
        line(out, "    cipher text    " + envelope.ciphertextWithTag().length + " bytes (" + statement.length
                + " + a 16-byte tag)");
        line(out, "    in all         " + envelope.totalLength() + " bytes");
        line(out, "  OPENING:");
        line(out, "    Ana's private key: " + Arrays.equals(HybridBox.open(ana.getPrivate(), ACCOUNT, envelope), statement));
        KeyPair eve = RsaBox.newKeyPair();
        line(out, "    Eve's private key: " + refused(() -> HybridBox.open(eve.getPrivate(), ACCOUNT, envelope)));
        byte[] flipped = envelope.ciphertextWithTag().clone();
        flipped[0] ^= 1;
        line(out, "    one bit of the cipher text flipped: "
                + refused(() -> HybridBox.open(ana.getPrivate(), ACCOUNT, new Envelope(envelope.lockedKey(), envelope.nonce(), flipped))));
    }

    private static void act4KeyAgreement(StringBuilder out) {
        line(out, "FOUR. Key agreement: no key is sent at all.");
        long g = 5;
        long p = 23;
        long bankSecret = 6;
        long anaSecret = 15;
        long bankSends = ByHand.power(g, bankSecret, p);
        long anaSends = ByHand.power(g, anaSecret, p);
        line(out, "  BY HAND, WITH TINY NUMBERS (everyone knows g = " + g + ", p = " + p + "):");
        line(out, "    bank secret " + bankSecret + "    sends " + g + "^" + bankSecret + " mod " + p + " = " + bankSends);
        line(out, "    Ana secret " + anaSecret + "    sends " + g + "^" + anaSecret + " mod " + p + " = " + anaSends);
        line(out, "    bank gets        " + anaSends + "^" + bankSecret + " mod " + p + " = " + ByHand.power(anaSends, bankSecret, p));
        line(out, "    Ana gets         " + bankSends + "^" + anaSecret + " mod " + p + " = " + ByHand.power(bankSends, anaSecret, p)
                + "  <- THE SAME, NEVER SENT");

        PrivateKey bankPrivate = KeyAgreementBox.privateKey(Hex.parse(BANK_PRIVATE));
        PublicKey bankPublic = KeyAgreementBox.publicKey(Hex.parse(BANK_PUBLIC));
        PrivateKey anaPrivate = KeyAgreementBox.privateKey(Hex.parse(ANA_PRIVATE));
        PublicKey anaPublic = KeyAgreementBox.publicKey(Hex.parse(ANA_PUBLIC));
        Wire wire = new Wire();
        wire.send(bankPublic.getEncoded());
        wire.send(anaPublic.getEncoded());
        line(out, "  BEFORE THE SWAP (real X25519 keys, from RFC 7748):");
        line(out, "    bank private  " + Hex.start(KeyAgreementBox.raw(bankPrivate), 16));
        line(out, "    bank public   " + Hex.start(KeyAgreementBox.raw(bankPublic), 16));
        line(out, "    Ana private   " + Hex.start(KeyAgreementBox.raw(anaPrivate), 16));
        line(out, "    Ana public    " + Hex.start(KeyAgreementBox.raw(anaPublic), 16));
        byte[] bankSecretBytes = KeyAgreementBox.sharedSecret(bankPrivate, anaPublic);
        byte[] anaSecretBytes = KeyAgreementBox.sharedSecret(anaPrivate, bankPublic);
        line(out, "  AFTER THE SWAP (own private + other's public key):");
        line(out, "    bank gets     " + Hex.start(bankSecretBytes, 16));
        line(out, "    Ana gets      " + Hex.start(anaSecretBytes, 16));
        line(out, "    the same secret: " + Arrays.equals(bankSecretBytes, anaSecretBytes));
        byte[] salt = Hex.bytes("session-1");
        byte[] bankKey = KeyAgreementBox.deriveKey(bankSecretBytes, salt, ACCOUNT);
        line(out, "  HKDF turns the secret into a " + bankKey.length + "-byte key:");
        line(out, "    key           " + Hex.start(bankKey, 16));
        line(out, "  on the wire: two public keys, " + wire.seenByEve().get(0).length + " bytes each, nothing else");
        KeyPair eve = KeyAgreementBox.newKeyPair();
        byte[] eveKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(eve.getPrivate(), anaPublic), salt, ACCOUNT);
        line(out, "  Eve, with a key pair of her own: the same key: " + Arrays.equals(eveKey, bankKey));
        KeyPair bank2 = KeyAgreementBox.newKeyPair();
        byte[] nextKey = KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(bank2.getPrivate(), anaPublic), salt, ACCOUNT);
        line(out, "  a new session, a new bank key: a different key: " + !Arrays.equals(nextKey, bankKey));
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: whose key is it?");
        KeyPair ana = RsaBox.newKeyPair();
        KeyPair mallory = RsaBox.newKeyPair();
        byte[] message = Hex.bytes("PIN is 4821");
        // Mallory sits on the wire and swaps Ana's public key for her own, and later passes the message on to Ana.
        Envelope toMallory = HybridBox.seal(mallory.getPublic(), ACCOUNT, message);
        byte[] read = HybridBox.open(mallory.getPrivate(), ACCOUNT, toMallory);
        Envelope onward = HybridBox.seal(ana.getPublic(), ACCOUNT, read);
        line(out, "  WHAT THE BANK SENDS:");
        line(out, "    message       " + Hex.text(message));
        line(out, "    locked to     Mallory's public key, swapped in for Ana's");
        line(out, "  IN THE MIDDLE, MALLORY:");
        line(out, "    reads         " + Hex.text(read));
        line(out, "    locks it again with Ana's real public key, sends it on");
        line(out, "  WHAT ANA RECEIVES:");
        line(out, "    opened        " + Hex.text(HybridBox.open(ana.getPrivate(), ACCOUNT, onward)) + "  <- NOBODY NOTICED");
        line(out, "  nothing in the maths says whose public key it is");
        KeyPair x = KeyAgreementBox.newKeyPair();
        line(out, "  public keys: RSA-2048 " + ana.getPublic().getEncoded().length + " bytes, X25519 "
                + x.getPublic().getEncoded().length + " bytes");
        line(out, "  a stolen private key opens every message locked to it");
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
