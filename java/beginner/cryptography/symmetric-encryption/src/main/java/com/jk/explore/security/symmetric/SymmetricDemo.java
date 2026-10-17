package com.jk.explore.security.symmetric;

import java.util.Arrays;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
public class SymmetricDemo {

    /** A demo key: 16 bytes written out so the output is repeatable. A real key is random and kept in a key store. */
    static final byte[] KEY = Hex.bytes("demo-key-16bytes");

    /** A statement of eight lines, each exactly 16 bytes, so each line is one AES block. */
    static final String STATEMENT = "SALARY    +4000\n" + "RENT      -1200\n" + "RENT      -1200\n" + "FOOD      -0310\n"
            + "RENT      -1200\n" + "SALARY    +4000\n" + "RENT      -1200\n" + "RENT      -1200\n";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Plain(out);
        act2Ecb(out);
        act3Gcm(out);
        act4NonceReuse(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Plain(StringBuilder out) {
        line(out, "ONE. A statement stored as it is.");
        line(out, "  a stolen disk shows the statement: " + STATEMENT.lines().count() + " lines, all readable");
        line(out, "  first line: " + STATEMENT.lines().findFirst().orElse("").trim() + "  <- NOTHING TO DECRYPT");
    }

    private static void act2Ecb(StringBuilder out) {
        line(out, "TWO. AES in ECB mode: every block on its own.");
        byte[] plain = Hex.bytes(STATEMENT);
        byte[] cipher = EcbCipher.encrypt(KEY, plain);
        line(out, "  the statement is " + plain.length / 16 + " blocks of 16 bytes");
        line(out, "  the plain text has " + EcbCipher.distinctBlocks(plain) + " different blocks, and so does the cipher text: "
                + EcbCipher.distinctBlocks(cipher));
        line(out, "  the attacker cannot read a line, but sees that 5 of 8 are the same payment  <- PATTERNS SHOW THROUGH");
        line(out, "  the first rent block begins " + Hex.of(Arrays.copyOfRange(cipher, 16, 32)).substring(0, 16)
                + ", the second rent block begins " + Hex.of(Arrays.copyOfRange(cipher, 32, 48)).substring(0, 16));
    }

    private static void act3Gcm(StringBuilder out) {
        line(out, "THREE. AES-GCM: hiding and checking.");
        GcmBox box = new GcmBox(KEY, new CountingNonces());
        byte[] account = Hex.bytes("account=ANA-1");
        byte[] line = Hex.bytes("RENT      -1200\n");
        Sealed one = box.seal(account, line);
        Sealed two = box.seal(account, line);
        line(out, "  the same line sealed twice: " + Hex.of(one.ciphertext()).substring(0, 16) + " and "
                + Hex.of(two.ciphertext()).substring(0, 16) + "  <- A NEW NONCE EACH TIME");
        line(out, "  opened with the right key and account: " + Hex.text(box.open(account, one)).trim());
        byte[] flipped = one.ciphertextWithTag().clone();
        flipped[0] ^= 0x01;
        line(out, "  one bit of the cipher text is flipped: " + tryOpen(box, account, new Sealed(one.nonce(), flipped)));
        line(out, "  the message is moved to another account: " + tryOpen(box, Hex.bytes("account=MAL-2"), one));
        GcmBox wrong = new GcmBox(Hex.bytes("another-key-16by"), new CountingNonces());
        line(out, "  opened with a different key: " + tryOpen(wrong, account, one));
    }

    private static void act4NonceReuse(StringBuilder out) {
        line(out, "FOUR. The one mistake: using a nonce twice.");
        GcmBox box = new GcmBox(KEY, new RepeatedNonce(new byte[12]));
        byte[] account = Hex.bytes("account=ANA-1");
        byte[] known = Hex.bytes("balance=000100");
        byte[] secret = Hex.bytes("balance=900100");
        Sealed first = box.seal(account, known);
        Sealed second = box.seal(account, secret);
        byte[] xorOfCiphers = Hex.xor(first.ciphertext(), second.ciphertext(), known.length);
        byte[] xorOfPlains = Hex.xor(known, secret, known.length);
        line(out, "  two messages sealed with the same key and the same nonce");
        line(out, "  cipher text one XOR cipher text two equals plain text one XOR plain text two: "
                + Arrays.equals(xorOfCiphers, xorOfPlains));
        byte[] recovered = Hex.xor(xorOfCiphers, known, known.length);
        line(out, "  the attacker knows the first message and recovers the second: " + Hex.text(recovered)
                + "  <- NO KEY WAS NEEDED");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: sizes, nonces, and the key.");
        GcmBox box = new GcmBox(KEY, new CountingNonces());
        byte[] account = Hex.bytes("account=ANA-1");
        int shortLength = box.seal(account, Hex.bytes("hello")).storedLength();
        int longLength = box.seal(account, Hex.bytes("x".repeat(500))).storedLength();
        line(out, "  5 bytes seal to " + shortLength + " bytes, 500 bytes to " + longLength + ": "
                + (shortLength - 5) + " bytes of overhead, and the length still shows  <- SIZE LEAKS");
        line(out, "  with random 12-byte nonces, a repeat becomes likely near " + (1L << 32) + " messages under one key");
        line(out, "  so the key must be replaced long before that");
        line(out, "  and the key must be stored somewhere safe: lose it, and every message is lost with it");
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
