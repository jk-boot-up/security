package com.jk.explore.security.pqc;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.DecapsulateException;
import javax.crypto.KEM;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Post-quantum cryptography in the JDK for a bank: sizes, ML-KEM, ML-DSA, a hybrid exchange with X25519,
 * Mosca's inequality and an inventory of a bank's code, each checked against Bouncy Castle where it can be.
 * The output has sizes, counts and yes or no answers only: no keys, no random values, no timings.
 */
public final class PostQuantumDemo {

    private static final StringBuilder OUT = new StringBuilder();
    static final String CARD_MESSAGE = "card 4111 declined";

    private PostQuantumDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static synchronized String run() throws Exception {
        OUT.setLength(0);
        sizes();
        mlKem();
        mlDsa();
        hybrid();
        planning();
        return OUT.toString();
    }

    private static void act(String heading) {
        OUT.append(heading).append('\n');
    }

    private static void section(String header) {
        OUT.append("  ").append(header).append('\n');
    }

    private static void line(String line) {
        OUT.append("    ").append(line).append('\n');
    }

    /** A key left-aligned in a column of the given width, then the value. */
    static String col(String key, int width, String value) {
        return key + " ".repeat(Math.max(1, width - key.length())) + value;
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    // ------------------------------------------------------------------ ONE: sizes

    private static void sizes() throws GeneralSecurityException {
        act("ONE. What has to cross the network.");
        List<Sizes.Exchange> ex = Sizes.exchanges();
        section("KEY EXCHANGE (bytes; public keys in X.509 form):");
        line(String.format("%-24s %10s %6s %7s", "algorithm", "public key", "reply", "secret"));
        for (Sizes.Exchange e : ex) {
            line(String.format("%-24s %10d %6d %7d", e.name(), e.publicKey(), e.reply(), e.secret()));
        }
        Sizes.Exchange x = Sizes.exchange(ex, "X25519");
        Sizes.Exchange k = Sizes.exchange(ex, "ML-KEM-768");
        section("HOW MUCH LARGER (ML-KEM-768 against X25519):");
        line(col("public key", 12, k.publicKey() + " / " + x.publicKey() + " = " + Sizes.times(k.publicKey(), x.publicKey()) + " times"));
        line(col("reply", 12, k.reply() + " / " + x.reply() + " = " + Sizes.times(k.reply(), x.reply()) + " times"));
        line(col("hybrid", 12, "client " + x.publicKey() + " + " + k.publicKey() + " = " + (x.publicKey() + k.publicKey())
                + ", server " + x.reply() + " + " + k.reply() + " = " + (x.reply() + k.reply())));
        List<Sizes.Signing> sg = Sizes.signings();
        section("SIGNATURES (bytes; public keys in X.509 form):");
        line(String.format("%-24s %10s %10s", "algorithm", "public key", "signature"));
        for (Sizes.Signing s : sg) {
            line(String.format("%-24s %10d %10d", s.name(), s.publicKey(), s.signature()));
        }
        Sizes.Signing ecdsa = Sizes.signing(sg, "ECDSA P-256");
        Sizes.Signing dsa = Sizes.signing(sg, "ML-DSA-65");
        section("ONE TLS HANDSHAKE (2 certificate keys + 3 signatures):");
        for (Sizes.Signing s : List.of(ecdsa, dsa)) {
            line(col(s.name(), 13, "2 x " + s.publicKey() + " + 3 x " + s.signature() + " = " + Sizes.chainBytes(s) + " bytes"));
        }
        line(col("larger by", 13, Sizes.chainBytes(dsa) + " / " + Sizes.chainBytes(ecdsa) + " = "
                + Sizes.times(Sizes.chainBytes(dsa), Sizes.chainBytes(ecdsa)) + " times"));
    }

    // ------------------------------------------------------------------ TWO: ML-KEM

    private static byte[] gcm(int mode, byte[] key, byte[] data) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        // One message per key, so a fixed nonce is acceptable in this demonstration.
        c.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, new byte[12]));
        return c.doFinal(data);
    }

    private static void mlKem() throws GeneralSecurityException {
        act("TWO. ML-KEM in the JDK.");
        KeyPair pair = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM");
        KEM.Encapsulated first = kem.newEncapsulator(pair.getPublic()).encapsulate();
        byte[] opened = kem.newDecapsulator(pair.getPrivate())
                .decapsulate(first.encapsulation()).getEncoded();
        section("THE SENDER (encapsulates to the bank's ML-KEM-768 key):");
        line(col("public key", 12, pair.getPublic().getEncoded().length + " bytes, algorithm " + pair.getPublic().getAlgorithm()));
        line(col("ciphertext", 12, first.encapsulation().length + " bytes, sent to the bank"));
        line(col("shared key", 12, first.key().getEncoded().length + " bytes, kept"));
        section("THE BANK (decapsulates with its private key):");
        line(col("shared key", 12, opened.length + " bytes, the same as the sender's: " + yes(Arrays.equals(opened, first.key().getEncoded()))));
        KEM.Encapsulated second = kem.newEncapsulator(pair.getPublic()).encapsulate();
        line(col("again", 12, "new ciphertext " + yes(!Arrays.equals(first.encapsulation(), second.encapsulation()))
                + ", new key " + yes(!Arrays.equals(first.key().getEncoded(), second.key().getEncoded()))));

        byte[] card = CARD_MESSAGE.getBytes(StandardCharsets.UTF_8);
        byte[] sealed = gcm(Cipher.ENCRYPT_MODE, first.key().getEncoded(), card);
        byte[] bad = first.encapsulation().clone();
        bad[bad.length / 2] ^= 1;
        boolean raised = false;
        byte[] other = null;
        try {
            other = kem.newDecapsulator(pair.getPrivate()).decapsulate(bad).getEncoded();
        } catch (DecapsulateException e) {
            raised = true;
        }
        String firstUse;
        try {
            gcm(Cipher.DECRYPT_MODE, other, sealed);
            firstUse = "decrypted";
        } catch (AEADBadTagException e) {
            firstUse = "AES-GCM tag check failed";
        }
        section("A TAMPERED CIPHERTEXT (one bit flipped):");
        line(col("message", 12, "\"" + CARD_MESSAGE + "\", sealed with the shared key"));
        line(col("error", 12, raised ? "raised" : "none raised (implicit rejection)"));
        line(col("key", 12, other.length + " bytes, differs from the sender's: " + yes(!Arrays.equals(other, first.key().getEncoded()))));
        line(col("first use", 12, firstUse));
        line(col("untouched", 12, "opens the message: " + yes(Arrays.equals(card,
                gcm(Cipher.DECRYPT_MODE, kem.newDecapsulator(pair.getPrivate()).decapsulate(first.encapsulation()).getEncoded(), sealed)))));
        String wrongLength;
        try {
            kem.newDecapsulator(pair.getPrivate()).decapsulate(Arrays.copyOf(first.encapsulation(), 10));
            wrongLength = "accepted";
        } catch (DecapsulateException e) {
            wrongLength = "DecapsulateException";
        }
        line(col("10 bytes", 12, wrongLength + " (a length error)"));
        section("THE JDK AND BOUNCY CASTLE 1.86 (ML-KEM-768):");
        line(col("public key", 12, "re-encoded byte for byte the same: " + yes(Interop.publicKeyEncodingsAgree())));
        line(col("JDK -> BC", 12, "encapsulate, decapsulate, same key: " + yes(Interop.jdkEncapsulatesBouncyCastleDecapsulates())));
        line(col("BC -> JDK", 12, "encapsulate, decapsulate, same key: " + yes(Interop.bouncyCastleEncapsulatesJdkDecapsulates())));
    }

    // ------------------------------------------------------------------ THREE: ML-DSA

    private static void mlDsa() throws GeneralSecurityException {
        act("THREE. ML-DSA in the JDK.");
        KeyPair pair = Signatures.generate("ML-DSA-65");
        byte[] statement = Signatures.statement();
        byte[] signature = Signatures.sign(pair, statement);
        section("THE STATEMENT (signed with ML-DSA-65):");
        line(new String(statement, StandardCharsets.UTF_8));
        line(col("public key", 12, pair.getPublic().getEncoded().length + " bytes"));
        line(col("signature", 12, signature.length + " bytes"));
        byte[] changed = statement.clone();
        changed[changed.length - 1] ^= 1;
        byte[] again = Signatures.sign(pair, statement);
        section("THE CHECKS (verify the signature):");
        line(col("as signed", 22, "verifies: " + yes(Signatures.verify(pair, statement, signature))));
        line(col("one bit changed", 22, "verifies: " + yes(Signatures.verify(pair, changed, signature))));
        line(col("another bank's key", 22, "verifies: " + yes(Signatures.verify(Signatures.generate("ML-DSA-65"), statement, signature))));
        line(col("signed a second time", 22, "different: " + yes(!Arrays.equals(signature, again))
                + ", verifies: " + yes(Signatures.verify(pair, statement, again))));
        line(col("ML-DSA-44 signature", 22, "with an ML-DSA-65 key: " + yes(verifiesAcrossSets(statement))));
        section("THE JDK AND BOUNCY CASTLE 1.86 (ML-DSA-65):");
        line(col("JDK signs", 13, "Bouncy Castle verifies: " + yes(Interop.jdkSignsBouncyCastleVerifies(statement))));
        line(col("BC signs", 13, "the JDK verifies: " + yes(Interop.bouncyCastleSignsJdkVerifies(statement))));
        line(col("private key", 13, "BC encoding read by the JDK: " + yes(Interop.jdkReadsBouncyCastlePrivateKey())));
    }

    private static boolean verifiesAcrossSets(byte[] statement) {
        try {
            KeyPair small = Signatures.generate("ML-DSA-44");
            KeyPair medium = Signatures.generate("ML-DSA-65");
            return Signatures.verify(medium, statement, Signatures.sign(small, statement));
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ FOUR: hybrid

    private static void hybrid() throws GeneralSecurityException {
        act("FOUR. A hybrid key exchange: X25519 and ML-KEM-768.");
        Hybrid.Exchange h = Hybrid.run();
        int cx = h.clientX().getPublic().getEncoded().length;
        int ck = h.clientK().getPublic().getEncoded().length;
        int sx = h.serverX().getPublic().getEncoded().length;
        int ct = h.ciphertext().length;
        section("WHAT CROSSES THE NETWORK:");
        line(col("client sends", 14, "X25519 key " + cx + " + ML-KEM key " + ck + " = " + (cx + ck) + " bytes"));
        line(col("server sends", 14, "X25519 key " + sx + " + ciphertext " + ct + " = " + (sx + ct) + " bytes"));
        byte[] info = Hybrid.info(h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        byte[] ikm = new byte[h.classicalSecret().length + h.kemSecret().length];
        System.arraycopy(h.classicalSecret(), 0, ikm, 0, h.classicalSecret().length);
        System.arraycopy(h.kemSecret(), 0, ikm, h.classicalSecret().length, h.kemSecret().length);
        byte[] prk = HkdfSteps.extract(ikm);
        byte[] byHand = HkdfSteps.expand(prk, info);
        section("HOW THE KEY IS MADE (HKDF-SHA256, worked by hand):");
        line(col("secrets", 9, "X25519 " + h.classicalSecret().length + " + ML-KEM " + h.kemSecret().length + " = " + ikm.length + " bytes"));
        line(col("context", 9, "label " + Hybrid.LABEL.length() + " + " + cx + " + " + ck + " + " + sx + " + " + ct + " = " + info.length + " bytes"));
        line(col("extract", 9, "HMAC(32 zero bytes, secrets) = " + prk.length + " bytes"));
        line(col("expand", 9, "HMAC(extracted, context + 01) = " + byHand.length + " bytes"));
        line(col("result", 9, "by hand = the JDK's KDF: " + yes(Hybrid.same(byHand, h.clientKey()))
                + ", client = server: " + yes(Hybrid.same(h.clientKey(), h.serverKey()))));
        line(col("", 9, "differs from X25519 alone: "
                + yes(!Hybrid.same(h.clientKey(), Hybrid.derive(h.classicalSecret(), new byte[0], new byte[0])))));
        // Simulation: the recorded exchange is attacked later. A quantum computer would recover an X25519 private key from its public key;
        // here the attacker is simply handed that private key, which is what the computer would produce.
        byte[] recovered = Hybrid.agree(h.clientX(), h.serverX());
        byte[] guess = Hybrid.combine(recovered, new byte[32], h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        byte[] classicalOnly = Hybrid.derive(recovered, new byte[0], new byte[0]);
        byte[] kemOnly = Hybrid.combine(new byte[32], h.kemSecret(), h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        byte[] otherContext = Hybrid.combine(h.classicalSecret(), h.kemSecret(), h.clientX(), h.clientK(), h.serverX(),
                Arrays.copyOf(h.ciphertext(), h.ciphertext().length - 1));
        section("A RECORDING, ATTACKED YEARS LATER:");
        line(col("X25519 broken", 15, "its secret recovered: " + yes(Hybrid.same(recovered, h.classicalSecret()))));
        line(col("", 15, "hybrid key rebuilt without ML-KEM: " + yes(Hybrid.same(guess, h.clientKey()))));
        line(col("", 15, "an X25519-only key rebuilt: "
                + yes(Hybrid.same(classicalOnly, Hybrid.derive(h.classicalSecret(), new byte[0], new byte[0])))));
        line(col("ML-KEM broken", 15, "hybrid key rebuilt without X25519: " + yes(Hybrid.same(kemOnly, h.clientKey()))));
        line(col("context cut", 15, "one ciphertext byte missing, key matches: " + yes(Hybrid.same(otherContext, h.clientKey()))));
    }

    // ------------------------------------------------------------------ FIVE: planning

    private static void planning() {
        act("FIVE. Planning the migration.");
        section("MOSCA'S INEQUALITY (made-up inputs, not a prediction):");
        line("exposed when x + y > z");
        line("x secrecy years, y migration years, z years to the threat");
        section("THE BANK'S DATA (x + y):");
        for (Mosca.DataClass d : Mosca.BANK) {
            line(col(d.name(), 38, String.format("%2d + %d = %2d", d.secrecyYears(), d.migrationYears(), d.needed())));
        }
        section("THE RESULT FOR EACH z (ok: years left to start; LATE: by):");
        StringBuilder head = new StringBuilder(String.format("%-20s", "data"));
        for (int z : Mosca.SCENARIOS) {
            head.append(String.format("%-10s", "z=" + z));
        }
        line(head.toString().stripTrailing());
        for (Mosca.DataClass d : Mosca.BANK) {
            StringBuilder row = new StringBuilder(String.format("%-20s", d.shortName()));
            for (int z : Mosca.SCENARIOS) {
                row.append(String.format("%-10s", d.exposed(z) ? "LATE " + (-d.startWithin(z)) : "ok " + d.startWithin(z)));
            }
            line(row.toString().stripTrailing());
        }
        StringBuilder exposed = new StringBuilder(String.format("%-20s", "exposed today"));
        for (int z : Mosca.SCENARIOS) {
            exposed.append(String.format("%-10s", Mosca.BANK.stream().filter(d -> d.exposed(z)).count() + " of " + Mosca.BANK.size()));
        }
        line(exposed.toString().stripTrailing());

        List<Inventory.Entry> entries = Inventory.scan(Inventory.BANK_SOURCES);
        section("THE INVENTORY (" + entries.size() + " source files of the bank, read as text):");
        for (Inventory.Entry e : entries) {
            String names = e.found().stream().map(Inventory.Algorithm::name).collect(Collectors.joining(", "));
            line(col(e.path(), 29, col(names, 15, e.first().priority() + " " + e.first().label)));
        }
        section("THE MIGRATION ORDER (by the first use in each file):");
        line(col("1 key exchange", 16, files(entries, Inventory.Use.KEY_EXCHANGE) + ": a recording opens later"));
        line(col("2 signature", 16, files(entries, Inventory.Use.SIGNATURE) + ": forged only once broken"));
        line(col("3 symmetric", 16, files(entries, Inventory.Use.SYMMETRIC) + ": keep; Grover only halves a key"));
    }

    private static String files(List<Inventory.Entry> entries, Inventory.Use use) {
        long n = Inventory.count(entries, use);
        return n + (n == 1 ? " file" : " files");
    }
}
