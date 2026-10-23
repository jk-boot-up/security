package com.jk.explore.security.pqc;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.List;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.DecapsulateException;
import javax.crypto.KEM;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Post-quantum cryptography in the JDK for a bank: ML-KEM and ML-DSA, a hybrid exchange with X25519, sizes, Mosca's inequality,
 * tampered ciphertext, an inventory of the repository's own classical cryptography and a check against Bouncy Castle.
 * The output has sizes, counts and yes or no answers only: no keys, no random values, no timings.
 */
public final class PostQuantumDemo {

    private static final StringBuilder OUT = new StringBuilder();

    private PostQuantumDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static synchronized String run() throws Exception {
        OUT.setLength(0);
        sizes();
        mlKem();
        tamper();
        mlDsa();
        hybrid();
        mosca();
        inventory();
        interop();
        return OUT.toString();
    }

    private static void say(String line) {
        OUT.append(line).append('\n');
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static void sizes() throws GeneralSecurityException {
        say("--- what has to cross the network (bytes, public keys in X.509 form)");
        List<Sizes.Exchange> ex = Sizes.exchanges();
        for (Sizes.Exchange e : ex) {
            say(String.format("%-24s public key %5d, reply %5d, shared secret %2d", e.name(), e.publicKey(), e.reply(), e.secret()));
        }
        Sizes.Exchange x = Sizes.exchange(ex, "X25519");
        Sizes.Exchange k = Sizes.exchange(ex, "ML-KEM-768");
        say("ML-KEM-768 public key compared with X25519: " + Sizes.times(k.publicKey(), x.publicKey()) + " times larger; ciphertext compared with the X25519 reply: "
                + Sizes.times(k.reply(), x.reply()) + " times");
        say("hybrid X25519 + ML-KEM-768: client sends " + (x.publicKey() + k.publicKey()) + " bytes, server replies " + (x.reply() + k.reply()) + " bytes");
        List<Sizes.Signing> sg = Sizes.signings();
        for (Sizes.Signing s : sg) {
            say(String.format("%-24s public key %5d, signature %5d", s.name(), s.publicKey(), s.signature()));
        }
        Sizes.Signing ecdsa = Sizes.signing(sg, "ECDSA P-256");
        Sizes.Signing dsa = Sizes.signing(sg, "ML-DSA-65");
        say("keys and signatures in a two-certificate chain plus the handshake signature: ECDSA P-256 " + Sizes.chainBytes(ecdsa) + " bytes, ML-DSA-65 "
                + Sizes.chainBytes(dsa) + " bytes (" + Sizes.times(Sizes.chainBytes(dsa), Sizes.chainBytes(ecdsa)) + " times)");
    }

    private static void mlKem() throws GeneralSecurityException {
        say("--- ML-KEM in the JDK (KeyPairGenerator ML-KEM-768 and javax.crypto.KEM)");
        KeyPair pair = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM");
        KEM.Encapsulated first = kem.newEncapsulator(pair.getPublic()).encapsulate();
        byte[] opened = kem.newDecapsulator(pair.getPrivate()).decapsulate(first.encapsulation()).getEncoded();
        say("ML-KEM encapsulation succeeded and both sides derived the same key: " + yes(Arrays.equals(opened, first.key().getEncoded())));
        say("ciphertext " + first.encapsulation().length + " bytes, shared secret " + opened.length + " bytes, key algorithm of the public key: " + pair.getPublic().getAlgorithm());
        KEM.Encapsulated second = kem.newEncapsulator(pair.getPublic()).encapsulate();
        say("a second encapsulation to the same public key gives a different ciphertext and a different secret: "
                + yes(!Arrays.equals(first.encapsulation(), second.encapsulation()) && !Arrays.equals(first.key().getEncoded(), second.key().getEncoded())));
    }

    private static byte[] gcm(int mode, byte[] key, byte[] data) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        // One message per key, so a fixed nonce is acceptable in this demonstration.
        c.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, new byte[12]));
        return c.doFinal(data);
    }

    private static void tamper() throws GeneralSecurityException {
        say("--- a tampered ciphertext (implicit rejection)");
        KeyPair pair = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM");
        KEM.Encapsulated e = kem.newEncapsulator(pair.getPublic()).encapsulate();
        byte[] card = "card 4111 declined".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] sealed = gcm(Cipher.ENCRYPT_MODE, e.key().getEncoded(), card);

        byte[] bad = e.encapsulation().clone();
        bad[bad.length / 2] ^= 1;
        boolean raised = false;
        byte[] other = null;
        try {
            other = kem.newDecapsulator(pair.getPrivate()).decapsulate(bad).getEncoded();
        } catch (DecapsulateException ex) {
            raised = true;
        }
        say("one bit flipped in the ciphertext, decapsulation raised an error: " + yes(raised));
        say("the receiver got a key of " + other.length + " bytes that differs from the sender's key: " + yes(!Arrays.equals(other, e.key().getEncoded())));
        String first;
        try {
            gcm(Cipher.DECRYPT_MODE, other, sealed);
            first = "decrypted";
        } catch (AEADBadTagException ex) {
            first = "AES-GCM tag check failed";
        }
        say("first use of that key on the sealed message: " + first);
        say("the untouched ciphertext opens the sealed message: " + yes(Arrays.equals(card,
                gcm(Cipher.DECRYPT_MODE, kem.newDecapsulator(pair.getPrivate()).decapsulate(e.encapsulation()).getEncoded(), sealed))));
        String wrongLength;
        try {
            kem.newDecapsulator(pair.getPrivate()).decapsulate(Arrays.copyOf(e.encapsulation(), 10));
            wrongLength = "accepted";
        } catch (DecapsulateException ex) {
            wrongLength = "DecapsulateException (only a wrong length is an error)";
        }
        say("a ciphertext of 10 bytes: " + wrongLength);
    }

    private static void mlDsa() throws GeneralSecurityException {
        say("--- ML-DSA in the JDK (KeyPairGenerator ML-DSA-65 and Signature ML-DSA)");
        KeyPair pair = Signatures.generate("ML-DSA-65");
        byte[] statement = Signatures.statement();
        byte[] signature = Signatures.sign(pair, statement);
        say("signature " + signature.length + " bytes, verifies: " + yes(Signatures.verify(pair, statement, signature)));
        byte[] changed = statement.clone();
        changed[changed.length - 1] ^= 1;
        say("statement changed by one bit, verifies: " + yes(Signatures.verify(pair, changed, signature)));
        say("verified with another bank's key, verifies: " + yes(Signatures.verify(Signatures.generate("ML-DSA-65"), statement, signature)));
        say("a second signature of the same statement differs from the first (hedged signing) and also verifies: "
                + yes(!Arrays.equals(signature, Signatures.sign(pair, statement)) && Signatures.verify(pair, statement, Signatures.sign(pair, statement))));
        say("a signature from ML-DSA-44 checked with an ML-DSA-65 key: " + yes(verifiesAcrossSets(statement)));
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

    private static void hybrid() throws GeneralSecurityException {
        say("--- hybrid key exchange (X25519 and ML-KEM-768 joined by HKDF-SHA256 with the transcript as context)");
        Hybrid.Exchange h = Hybrid.run();
        say("client and server derived the same 32-byte key: " + yes(Hybrid.same(h.clientKey(), h.serverKey())));
        say("the hybrid key differs from the key X25519 alone would give: "
                + yes(!Hybrid.same(h.clientKey(), Hybrid.derive(h.classicalSecret(), new byte[0], new byte[0]))));
        // Simulation: the recorded exchange is attacked later. A quantum computer would recover an X25519 private key from its public key;
        // here the attacker is simply handed that private key, which is what the computer would produce.
        byte[] recovered = Hybrid.agree(h.clientX(), h.serverX());
        say("recorded exchange, X25519 broken (the attacker is given the X25519 secret): recovers the X25519 secret " + yes(Hybrid.same(recovered, h.classicalSecret())));
        byte[] guess = Hybrid.combine(recovered, new byte[32], h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        say("  a hybrid key rebuilt from it without the ML-KEM secret matches: " + yes(Hybrid.same(guess, h.clientKey())));
        byte[] classicalOnly = Hybrid.derive(recovered, new byte[0], new byte[0]);
        say("  a classical-only exchange (HKDF of the X25519 secret alone) would be rebuilt: " + yes(Hybrid.same(classicalOnly, Hybrid.derive(h.classicalSecret(), new byte[0], new byte[0]))));
        byte[] kemOnly = Hybrid.combine(new byte[32], h.kemSecret(), h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        say("recorded exchange, ML-KEM broken (the attacker is given the ML-KEM secret): a hybrid key rebuilt without the X25519 secret matches: " + yes(Hybrid.same(kemOnly, h.clientKey())));
        byte[] otherContext = Hybrid.combine(h.classicalSecret(), h.kemSecret(), h.clientX(), h.clientK(), h.serverX(), Arrays.copyOf(h.ciphertext(), h.ciphertext().length - 1));
        say("same two secrets, one ciphertext byte missing from the transcript: the key matches " + yes(Hybrid.same(otherContext, h.clientKey())));
        say("the hybrid key is as strong as the stronger of the two parts, at the cost of both sets of bytes");
    }

    private static void mosca() {
        say("--- Mosca's inequality (an illustration with made-up inputs, not a prediction): exposed when secrecy years + migration years > years until the threat");
        say("threat arrival scenarios in years: " + Arrays.toString(Mosca.SCENARIOS));
        for (Mosca.DataClass d : Mosca.BANK) {
            StringBuilder line = new StringBuilder(String.format("%-38s secrecy %2d + migration %d = %2d:", d.name(), d.secrecyYears(), d.migrationYears(), d.needed()));
            for (int z : Mosca.SCENARIOS) {
                line.append(String.format(" z=%d %s", z, d.exposed(z) ? "EXPOSED (late by " + (-d.startWithin(z)) + ")" : "ok (start within " + d.startWithin(z) + ")"));
                line.append(';');
            }
            say(line.substring(0, line.length() - 1));
        }
        for (int z : Mosca.SCENARIOS) {
            long exposed = Mosca.BANK.stream().filter(d -> d.exposed(z)).count();
            say("if the threat arrives in " + z + " years: " + exposed + " of " + Mosca.BANK.size() + " data classes already exposed to recording today");
        }
    }

    private static void inventory() throws Exception {
        say("--- inventory of classical public-key cryptography in this repository (main source of the beginner and intermediate tracks)");
        Path root = Inventory.javaRoot(Path.of(""));
        if (root == null) {
            say("repository not found from the working directory");
            return;
        }
        Inventory.Result r = Inventory.scan(root);
        say("scanned " + r.files() + " source files in " + r.projects() + " projects");
        say("projects naming each algorithm (a name is not proof of use, but it is where to look):");
        for (String name : Inventory.PUBLIC_KEY.keySet()) {
            say(String.format("  %-30s %3d  (broken by a large quantum computer)", name, r.projectsByAlgorithm().get(name).size()));
        }
        for (String name : Inventory.SYMMETRIC.keySet()) {
            say(String.format("  %-30s %3d  (not broken by Shor; Grover halves the key strength)", name, r.projectsByAlgorithm().get(name).size()));
        }
        say("projects naming any classical public-key algorithm: " + r.anyPublicKey().size() + " of " + r.projects());
        say("projects naming ML-KEM: " + r.mlKem() + ", ML-DSA: " + r.mlDsa());
        say("migration order: 1) key exchange and key transport, because recorded traffic can be opened later; 2) long-lived signatures (roots, code signing, archives); 3) the rest");
    }

    private static void interop() throws GeneralSecurityException {
        say("--- the JDK and Bouncy Castle 1.86 agree");
        say("a Bouncy Castle ML-KEM-768 public key re-encoded by the JDK is byte for byte the same: " + yes(Interop.publicKeyEncodingsAgree()));
        say("JDK encapsulates, Bouncy Castle decapsulates, same key: " + yes(Interop.jdkEncapsulatesBouncyCastleDecapsulates()));
        say("Bouncy Castle encapsulates, JDK decapsulates, same key: " + yes(Interop.bouncyCastleEncapsulatesJdkDecapsulates()));
        say("JDK signs with ML-DSA-65, Bouncy Castle verifies: " + yes(Interop.jdkSignsBouncyCastleVerifies(Signatures.statement())));
        say("Bouncy Castle signs with ML-DSA-65, JDK verifies: " + yes(Interop.bouncyCastleSignsJdkVerifies(Signatures.statement())));
        say("the JDK reads a Bouncy Castle private key encoding: " + yes(Interop.jdkReadsBouncyCastlePrivateKey()) + " (public keys, ciphertexts and signatures cross; private keys stay where they were made)");
    }
}
