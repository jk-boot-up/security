package com.jk.explore.security.symmetric;

import java.util.Arrays;

/** Runs the online bank story in eight numbered acts and prints exact, repeatable results, byte by byte. */
public class SymmetricDemo {

    /** A demo key: 16 bytes written out so the output is repeatable. A real key is random and kept in a key store. */
    static final byte[] KEY = Hex.bytes("demo-key-16bytes");

    /** A statement of eight lines, each exactly 16 bytes, so each line is one AES block. */
    static final String STATEMENT = "SALARY    +4000\n" + "RENT      -1200\n" + "RENT      -1200\n" + "FOOD      -0310\n"
            + "RENT      -1200\n" + "SALARY    +4000\n" + "RENT      -1200\n" + "RENT      -1200\n";

    /** The associated data of every sealed line: the account it belongs to. Not hidden, but checked. */
    static final String ACCOUNT = "account=ANA-1";

    /** The line that the GCM acts seal and open. */
    static final String LINE = "RENT      -1200\n";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Plain(out);
        act2Ecb(out);
        GcmBox box = new GcmBox(KEY, new CountingNonces());
        Sealed first = act3Seal(out, box);
        act4Tag(out, first);
        act5SealAgain(out, box, first);
        act6Open(out, box, first);
        act7NonceReuse(out);
        act8Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Plain(StringBuilder out) {
        line(out, "ONE. A statement stored as it is.");
        line(out, "  a stolen disk shows the statement: " + STATEMENT.lines().count() + " lines, all readable");
        int n = 1;
        for (String l : STATEMENT.lines().toList()) {
            line(out, "  line " + n++ + "  " + l);
        }
        line(out, "  first line: " + STATEMENT.lines().findFirst().orElse("").trim() + "  <- NOTHING TO DECRYPT");
    }

    private static void act2Ecb(StringBuilder out) {
        line(out, "TWO. AES in ECB mode: every block on its own.");
        byte[] plain = Hex.bytes(STATEMENT);
        byte[] cipher = EcbCipher.encrypt(KEY, plain);
        line(out, "  key  " + Hex.grouped(KEY));
        line(out, "  BEFORE ENCRYPTION (plain text, one line per block):");
        for (int i = 0; i < plain.length; i += EcbCipher.BLOCK) {
            line(out, "    line " + (i / EcbCipher.BLOCK + 1) + "  " + Hex.text(Arrays.copyOfRange(plain, i, i + EcbCipher.BLOCK)).trim());
        }
        line(out, "  AFTER ENCRYPTION (cipher text, block by block):");
        for (int i = 0; i < cipher.length; i += EcbCipher.BLOCK) {
            line(out, "    line " + (i / EcbCipher.BLOCK + 1) + "  " + Hex.grouped(Arrays.copyOfRange(cipher, i, i + EcbCipher.BLOCK)));
        }
        line(out, "  " + EcbCipher.distinctBlocks(plain) + " different lines in, " + EcbCipher.distinctBlocks(cipher) + " different blocks out");
        line(out, "  5 of 8 blocks are the same payment  <- PATTERNS SHOW THROUGH");
    }

    private static Sealed act3Seal(StringBuilder out, GcmBox box) {
        line(out, "THREE. AES-GCM, step by step: sealing one line.");
        byte[] plain = Hex.bytes(LINE);
        Sealed sealed = box.seal(Hex.bytes(ACCOUNT), plain);
        line(out, "  BEFORE ENCRYPTION (the inputs):");
        line(out, "    key          " + Hex.grouped(KEY));
        line(out, "    nonce        " + Hex.grouped(sealed.nonce()));
        line(out, "    account      " + ACCOUNT);
        line(out, "    plain text   " + LINE.trim());
        line(out, "    as bytes     " + Hex.grouped(plain));
        line(out, "  AFTER ENCRYPTION (the outputs):");
        line(out, "    cipher text  " + Hex.grouped(sealed.ciphertext()));
        line(out, "    tag          " + Hex.grouped(tag(sealed)));
        line(out, "  stored: nonce + cipher text + tag = " + sealed.nonce().length + " + " + sealed.ciphertext().length + " + "
                + GcmBox.TAG_BYTES + " = " + sealed.storedLength() + " bytes");
        return sealed;
    }

    private static void act4Tag(StringBuilder out, Sealed sealed) {
        line(out, "FOUR. How the tag is made, step by step.");
        TagSteps.Steps s = TagSteps.of(KEY, sealed.nonce(), Hex.bytes(ACCOUNT), sealed.ciphertext());
        line(out, "  STEP 1. A HASH KEY H = AES(key, 16 zero bytes):");
        line(out, "    H          " + Hex.grouped(s.hashKey()));
        line(out, "  STEP 2. STIR EACH BLOCK INTO X:  X = (X xor block) times H");
        line(out, "    start X    " + Hex.grouped(new byte[16]));
        for (TagSteps.Mix m : s.mixes()) {
            line(out, "    " + String.format("%-11s", m.name().equals("cipher text") ? "cipher" : m.name()) + Hex.grouped(m.block()));
            line(out, "      X =      " + Hex.grouped(m.after()));
        }
        line(out, "  STEP 3. A MASK = AES(key, nonce + 00000001):");
        line(out, "    counter    " + Hex.grouped(s.counterBlock()));
        line(out, "    mask       " + Hex.grouped(s.mask()));
        line(out, "  STEP 4. TAG = X xor MASK:");
        line(out, "    tag        " + Hex.grouped(s.tag()));
        line(out, "    the same tag GCM gave in act three: " + Arrays.equals(s.tag(), tag(sealed)));
    }

    private static void act5SealAgain(StringBuilder out, GcmBox box, Sealed first) {
        line(out, "FIVE. The same line, sealed again.");
        Sealed again = box.seal(Hex.bytes(ACCOUNT), Hex.bytes(LINE));
        line(out, "  BEFORE ENCRYPTION (the same both times):");
        line(out, "    plain text   " + LINE.trim());
        line(out, "    nonce 1      " + Hex.grouped(first.nonce()));
        line(out, "    nonce 2      " + Hex.grouped(again.nonce()));
        line(out, "  AFTER ENCRYPTION:");
        line(out, "    with nonce 1 " + Hex.grouped(first.ciphertext()));
        line(out, "    with nonce 2 " + Hex.grouped(again.ciphertext()));
        line(out, "  new nonce, different cipher text: " + !Arrays.equals(first.ciphertext(), again.ciphertext())
                + "  <- A NEW NONCE EACH TIME");
    }

    private static void act6Open(StringBuilder out, GcmBox box, Sealed sealed) {
        line(out, "SIX. Opening it, and three changes.");
        byte[] account = Hex.bytes(ACCOUNT);
        line(out, "  THE SEALED LINE (from act three):");
        line(out, "    cipher text  " + Hex.grouped(sealed.ciphertext()));
        line(out, "    tag          " + Hex.grouped(tag(sealed)));
        line(out, "  OPENING IT (the tag is checked first):");
        line(out, "    right key, right account: " + Hex.text(box.open(account, sealed)).trim() + "  <- OPENED");
        line(out, "  THREE CHANGES:");
        byte[] flipped = sealed.ciphertextWithTag().clone();
        flipped[0] ^= 0x01;
        line(out, "    one bit flipped, " + Hex.of(new byte[] {sealed.ciphertextWithTag()[0]}) + " -> "
                + Hex.of(new byte[] {flipped[0]}) + ": " + tryOpen(box, account, new Sealed(sealed.nonce(), flipped)));
        line(out, "    account changed to account=MAL-2: " + tryOpen(box, Hex.bytes("account=MAL-2"), sealed));
        GcmBox wrong = new GcmBox(Hex.bytes("another-key-16by"), new CountingNonces());
        line(out, "    opened with another key: " + tryOpen(wrong, account, sealed));
    }

    private static void act7NonceReuse(StringBuilder out) {
        line(out, "SEVEN. The one mistake: using a nonce twice.");
        GcmBox box = new GcmBox(KEY, new RepeatedNonce(new byte[12]));
        byte[] account = Hex.bytes(ACCOUNT);
        byte[] known = Hex.bytes("balance=000100");
        byte[] secret = Hex.bytes("balance=900100");
        Sealed first = box.seal(account, known);
        Sealed second = box.seal(account, secret);
        byte[] xorOfCiphers = Hex.xor(first.ciphertext(), second.ciphertext(), known.length);
        byte[] xorOfPlains = Hex.xor(known, secret, known.length);
        line(out, "  the same nonce twice  " + Hex.grouped(first.nonce()));
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    plain text 1   " + Hex.text(known) + "   (the attacker knows it)");
        line(out, "    plain text 2   " + Hex.text(secret) + "   (the secret)");
        line(out, "  AFTER ENCRYPTION:");
        line(out, "    cipher text 1  " + Hex.grouped(first.ciphertext()));
        line(out, "    cipher text 2  " + Hex.grouped(second.ciphertext()));
        line(out, "  THE ATTACK, WITHOUT THE KEY:");
        line(out, "    cipher 1 XOR cipher 2  " + Hex.grouped(xorOfCiphers));
        line(out, "    plain 1 XOR plain 2    " + Hex.grouped(xorOfPlains));
        line(out, "    the two are equal: " + Arrays.equals(xorOfCiphers, xorOfPlains));
        byte[] recovered = Hex.xor(xorOfCiphers, known, known.length);
        line(out, "    XOR with plain text 1: " + Hex.text(recovered) + "  <- NO KEY WAS NEEDED");
    }

    private static void act8Bill(StringBuilder out) {
        line(out, "EIGHT. The bill: sizes, nonces, and the key.");
        GcmBox box = new GcmBox(KEY, new CountingNonces());
        byte[] account = Hex.bytes(ACCOUNT);
        int shortLength = box.seal(account, Hex.bytes("hello")).storedLength();
        int longLength = box.seal(account, Hex.bytes("x".repeat(500))).storedLength();
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    message 1  \"hello\"        5 bytes");
        line(out, "    message 2  500 letters    500 bytes");
        line(out, "  AFTER ENCRYPTION (nonce + cipher text + tag):");
        line(out, "    message 1  12 + 5 + 16   = " + shortLength + " bytes");
        line(out, "    message 2  12 + 500 + 16 = " + longLength + " bytes  <- SIZE LEAKS");
        line(out, "  the content is hidden, the length is not");
        line(out, "  random 12-byte nonces: at most " + (1L << 32) + " messages per key");
        line(out, "  so the key must be replaced long before that");
        line(out, "  and the key must be kept safe: lose it, lose every message");
    }

    private static byte[] tag(Sealed sealed) {
        byte[] all = sealed.ciphertextWithTag();
        return Arrays.copyOfRange(all, all.length - GcmBox.TAG_BYTES, all.length);
    }

    private static String tryOpen(GcmBox box, byte[] associatedData, Sealed sealed) {
        try {
            box.open(associatedData, sealed);
            return "opened";
        } catch (GcmBox.TamperedException e) {
            return "refused  <- DETECTED";
        }
    }
}
