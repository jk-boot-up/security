package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Runs the online store's release story in five numbered acts and prints exact, repeatable results. Each act prints labelled
 * sections in capitals: the input first, then the result, and every derived value worked out with the real numbers.
 */
public class SigningDemo {

    static final String NAME = "shop-app";
    static final String STABLE = "stable";
    static final int CHANGED_BYTE = 700;

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Trusting(out);
        act2Signed(out);
        act3Keys(out);
        act4Log(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A section header, two spaces in. */
    private static void section(StringBuilder out, String text) {
        line(out, "  " + text);
    }

    /** A "label  value" row, four spaces in, the label padded to a column. */
    private static void row(StringBuilder out, int width, String label, String value) {
        line(out, "    " + (width == 0 ? "" : String.format("%-" + width + "s", label)) + value);
    }

    /** A "what arrives  verdict  reason" row. */
    private static void verdict(StringBuilder out, int width, String label, Verdict verdict) {
        row(out, width, label, String.format("%-10s", verdict.word()) + verdict.reason());
    }

    private static String shown(byte b) {
        return "'" + (char) b + "' (" + String.format("0x%02x", b) + ")";
    }

    private static void act1Trusting(StringBuilder out) {
        line(out, "ONE. A deployment that runs whatever arrives.");
        Artifact built = Artifact.shopApp("2.4.0");
        Artifact mirrored = built.withByteChanged(CHANGED_BYTE);
        section(out, "BEFORE THE MIRROR (what the release team built):");
        row(out, 10, "file", built.fileName() + ", " + built.bytes().length + " bytes");
        row(out, 10, "byte " + CHANGED_BYTE, shown(built.bytes()[CHANGED_BYTE]));
        row(out, 10, "sha256", Digests.shortHex(built.sha256()) + "...");
        section(out, "AFTER THE MIRROR (what the deployment downloaded):");
        row(out, 10, "file", mirrored.fileName() + ", " + mirrored.bytes().length + " bytes");
        row(out, 10, "byte " + CHANGED_BYTE, shown(mirrored.bytes()[CHANGED_BYTE]) + "  <- ONE BIT FLIPPED");
        row(out, 10, "sha256", Digests.shortHex(mirrored.sha256()) + "...  <- A NEW DIGEST");
        section(out, "THE TRUSTING DEPLOYMENT STEP:");
        verdict(out, 0, "", DeployGate.trusting(mirrored));
        line(out, "    <- AN ALTERED JAR IS RUNNING");
        section(out, "WHY A DIGEST NEXT TO THE FILE DOES NOT HELP:");
        line(out, "    whoever can change the file can change the digest beside it");
    }

    private static void act2Signed(StringBuilder out) {
        line(out, "TWO. A signed release, checked before it runs.");
        SigningKey key = SigningKey.demo("stable-2026a", STABLE);
        Artifact jar = Artifact.shopApp("2.4.0");
        Statement statement = Statement.of(jar, NAME, "2.4.0", STABLE, 120);
        SignedRelease release = key.sign(statement);
        Verifier verifier = new Verifier(new KeyRing().trust(key, 0));
        byte[] text = statement.bytes();
        section(out, "BEFORE SIGNING (the statement, " + text.length + " bytes of text):");
        for (String field : new String(text, StandardCharsets.UTF_8).split("\n")) {
            line(out, "    " + (field.startsWith("sha256=") ? field.substring(0, 23) + "... (64 hex digits)" : field));
        }
        section(out, "AFTER SIGNING (Ed25519 with the key stable-2026a):");
        row(out, 12, "public key", Digests.shortHex(Ed25519.raw(key.publicKey())) + "... (" + Ed25519.raw(key.publicKey()).length + " bytes)");
        row(out, 12, "signature", Digests.shortHex(release.signature()) + "... (" + release.signature().length + " bytes)");
        row(out, 12, "again", Digests.same(release.signature(), key.sign(statement).signature())
                ? "the same signature: Ed25519 is deterministic" : "a different signature");
        section(out, "HOW THE DEPLOYMENT CHECKS (it pins stable-2026a):");
        row(out, 11, "1 parse", "the statement is in its one exact form");
        row(out, 11, "2 key", "the key id is one the deployment pinned");
        row(out, 11, "3 verify", "Ed25519(public key, statement, signature)");
        row(out, 11, "4 channel", "the key and the statement are for stable");
        row(out, 11, "5 dates", "signed while the key was trusted");
        row(out, 11, "6 digest", "sha256(jar) equals the signed sha256");
        line(out, "  THE RESULTS:");
        Statement edited = new Statement(NAME, "2.4.1", STABLE, statement.sha256(), 120);
        SigningKey forger = SigningKey.demo("stable-2026a", STABLE, "a forger's own key");
        SigningKey unknown = SigningKey.demo("attacker-1", STABLE);
        verdict(out, 18, "the real jar", verifier.verify(jar, release, NAME, STABLE));
        verdict(out, 18, "one byte changed", verifier.verify(jar.withByteChanged(CHANGED_BYTE), release, NAME, STABLE));
        verdict(out, 18, "edited to 2.4.1", verifier.verify(jar, new SignedRelease(edited.bytes(), release.keyId(), release.signature()), NAME, STABLE));
        verdict(out, 18, "a forged key", verifier.verify(jar, forger.sign(statement), NAME, STABLE));
        verdict(out, 18, "an unknown key", verifier.verify(jar, unknown.sign(statement), NAME, STABLE));
        verdict(out, 18, "no signature", verifier.verify(jar, null, NAME, STABLE));
    }

    private static void act3Keys(StringBuilder out) {
        line(out, "THREE. Signing keys: per channel, rotated, revoked.");
        SigningKey stableA = SigningKey.demo("stable-2026a", STABLE);
        SigningKey stableB = SigningKey.demo("stable-2026b", STABLE);
        SigningKey beta = SigningKey.demo("beta-2026a", "beta");
        KeyRing ring = new KeyRing().trust(stableA, 0).trust(beta, 0);
        Verifier verifier = new Verifier(ring);
        Artifact betaJar = Artifact.shopApp("2.5.0-beta1");
        section(out, "A BETA BUILD, OFFERED TO THE STABLE DEPLOYMENT:");
        verdict(out, 12, "beta-2026a", verifier.verify(betaJar, beta.sign(Statement.of(betaJar, NAME, "2.5.0-beta1", "beta", 105)), NAME, STABLE));
        ring.trust(stableB, 100);
        ring.revoke("stable-2026a", 130);
        KeyRing.TrustedKey a = ring.find("stable-2026a").orElseThrow();
        KeyRing.TrustedKey b = ring.find("stable-2026b").orElseThrow();
        section(out, "THE ROTATION (the stable keys, by day):");
        row(out, 14, a.keyId(), "trusted from day " + a.validFromDay() + ", revoked on day " + a.revokedFromDay());
        row(out, 14, b.keyId(), "added on day " + b.validFromDay());
        row(out, 14, "the overlap", a.revokedFromDay() + " - " + b.validFromDay() + " = " + (a.revokedFromDay() - b.validFromDay())
                + " days, day " + b.validFromDay() + " to " + (a.revokedFromDay() - 1));
        section(out, "SIGNED ON A DAY, THE VERDICT:");
        Artifact jar = Artifact.shopApp("2.4.0");
        int[][] cases = {{110, 'a'}, {110, 'b'}, {120, 'a'}, {140, 'a'}};
        for (int[] c : cases) {
            SigningKey key = c[1] == 'a' ? stableA : stableB;
            verdict(out, 23, "day " + c[0] + "  " + key.keyId(), verifier.verify(jar, key.sign(Statement.of(jar, NAME, "2.4.0", STABLE, c[0])), NAME, STABLE));
        }
        Artifact other = jar.withByteChanged(900);
        section(out, "BEFORE: A THIEF WITH stable-2026a, ON DAY 150:");
        row(out, 10, "jar", "a different one, sha256 " + Digests.shortHex(other.sha256()) + "...");
        row(out, 10, "writes", "signed-day=120, before the revocation");
        section(out, "AFTER: THE VERDICT:");
        verdict(out, 0, "", verifier.verify(other, stableA.sign(Statement.of(other, NAME, "2.4.0", STABLE, 120)), NAME, STABLE));
        line(out, "    <- A BACKDATED SIGNATURE: THE SIGNER WRITES THE DAY");
    }

    private static void act4Log(StringBuilder out) {
        line(out, "FOUR. A public log that every signature must be in.");
        SigningKey stableA = SigningKey.demo("stable-2026a", STABLE);
        SigningKey beta = SigningKey.demo("beta-2026a", "beta");
        KeyRing ring = new KeyRing().trust(stableA, 0).trust(beta, 0);
        TransparencyLog log = TransparencyLog.demo();
        Artifact old = Artifact.shopApp("2.3.0");
        Artifact jar = Artifact.shopApp("2.4.0");
        SignedRelease r1 = stableA.sign(Statement.of(old, NAME, "2.3.0", STABLE, 90));
        SignedRelease r2 = stableA.sign(Statement.of(jar, NAME, "2.4.0", STABLE, 120));
        log.append(r1, 90);
        log.append(r2, 120);
        TransparencyLog.TreeHead seen = log.head();
        byte[] leaf0 = MerkleTree.leafHash(log.entry(0));
        byte[] leaf1 = MerkleTree.leafHash(log.entry(1));
        section(out, "THE LOG (each entry: key id, statement, signature):");
        row(out, 9, "entry 0", "2.3.0 by stable-2026a, logged day " + log.dayOf(0) + ", " + log.entry(0).length + " bytes");
        row(out, 9, "entry 1", "2.4.0 by stable-2026a, logged day " + log.dayOf(1) + ", " + log.entry(1).length + " bytes");
        section(out, "HOW THE ROOT IS MADE (a Merkle tree, RFC 9162):");
        row(out, 8, "leaf 0", "sha256(00 || entry 0)       = " + Digests.shortHex(leaf0));
        row(out, 8, "leaf 1", "sha256(00 || entry 1)       = " + Digests.shortHex(leaf1));
        row(out, 8, "root", "sha256(01 || leaf 0 || leaf 1) = " + Digests.shortHex(MerkleTree.nodeHash(leaf0, leaf1)));
        section(out, "THE SIGNED HEAD:");
        row(out, 8, "size", String.valueOf(seen.size()));
        row(out, 8, "root", Digests.shortHex(seen.root()) + "...");
        row(out, 8, "signed", "by the log's own key: " + TransparencyLog.headIsSigned(seen, log.publicKey()));
        List<byte[]> proof = log.inclusionProof(1, seen.size());
        section(out, "THE PROOF THAT ENTRY 1 IS IN THE LOG (" + proof.size() + " hash):");
        row(out, 10, "proof", "leaf 0 = " + Digests.shortHex(proof.getFirst()));
        row(out, 10, "redo", "sha256(01 || proof || leaf 1) = " + Digests.shortHex(MerkleTree.nodeHash(proof.getFirst(), leaf1)));
        row(out, 10, "checks", "equals the signed root: "
                + MerkleTree.verifyInclusion(MerkleTree.leafHash(r2.logEntry()), 1, seen.size(), proof, seen.root()));

        DeployGate gate = new DeployGate(ring, log);
        ring.revoke("stable-2026a", 130);
        Artifact other = jar.withByteChanged(900);
        SignedRelease backdated = stableA.sign(Statement.of(other, NAME, "2.4.0", STABLE, 120));
        section(out, "THE DEPLOYMENT, NOW REQUIRING A LOG ENTRY:");
        verdict(out, 18, "the real 2.4.0", gate.checkWithLog(jar, r2, NAME, STABLE));
        verdict(out, 18, "thief, not logged", gate.checkWithLog(other, backdated, NAME, STABLE));
        TransparencyLog customerView = log.fork();
        TransparencyLog monitorView = log;
        customerView.append(backdated, 150);
        verdict(out, 18, "thief, logged", new DeployGate(ring, customerView).checkWithLog(other, backdated, NAME, STABLE));
        line(out, "    <- THE LOG'S DAY COUNTS, NOT THE DAY THE SIGNER WROTE");
        Monitor monitor = new Monitor(ring, Set.of(Digests.hex(old.sha256()), Digests.hex(jar.sha256())));
        section(out, "THE STORE'S MONITOR (signed with its keys, never released):");
        for (String finding : monitor.unexpected(customerView)) {
            line(out, "    " + finding);
        }

        Artifact betaJar = Artifact.shopApp("2.5.0-beta1");
        monitorView.append(beta.sign(Statement.of(betaJar, NAME, "2.5.0-beta1", "beta", 140)), 150);
        TransparencyLog.TreeHead toMonitor = monitorView.head();
        TransparencyLog.TreeHead toCustomer = customerView.head();
        section(out, "A LOG THAT HIDES ENTRY 2 SHOWS TWO VIEWS OF SIZE " + toMonitor.size() + ":");
        row(out, 10, "monitor", "entry 2 = the beta build, root " + Digests.shortHex(toMonitor.root()));
        row(out, 10, "customer", "entry 2 = the thief's jar, root " + Digests.shortHex(toCustomer.root()));
        section(out, "HOW EACH VIEW EXTENDS THE HEAD OF SIZE " + seen.size() + " (proof: leaf 2):");
        byte[] monitorLeaf = MerkleTree.leafHash(monitorView.entry(2));
        byte[] customerLeaf = MerkleTree.leafHash(customerView.entry(2));
        row(out, 10, "monitor", "sha256(01 || " + Digests.shortHex(seen.root()).substring(0, 8) + " || " + Digests.shortHex(monitorLeaf).substring(0, 8)
                + ") = " + Digests.shortHex(MerkleTree.nodeHash(seen.root(), monitorLeaf)));
        row(out, 10, "customer", "sha256(01 || " + Digests.shortHex(seen.root()).substring(0, 8) + " || " + Digests.shortHex(customerLeaf).substring(0, 8)
                + ") = " + Digests.shortHex(MerkleTree.nodeHash(seen.root(), customerLeaf)));
        row(out, 10, "both", "consistent with the old head: "
                + TransparencyLog.consistent(seen, toMonitor, monitorView.consistencyProof(seen.size(), toMonitor.size())) + " and "
                + TransparencyLog.consistent(seen, toCustomer, customerView.consistencyProof(seen.size(), toCustomer.size())));
        boolean fork = toMonitor.size() == toCustomer.size() && !Digests.same(toMonitor.root(), toCustomer.root());
        section(out, "THE MONITOR AND THE CUSTOMER COMPARE HEADS:");
        row(out, 11, "same size", toMonitor.size() + " = " + toCustomer.size() + ", but two different roots, both signed");
        row(out, 11, "fork", "proved: " + fork + "  <- ONE LOG, TWO HISTORIES");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        SigningKey key = SigningKey.demo("stable-2026a", STABLE);
        DeployGate gate = new DeployGate(new KeyRing().trust(key, 0), TransparencyLog.demo());
        section(out, "THE GATE GUARDS ONE PATH:");
        verdict(out, 18, "an unsigned jar", gate.checkWithLog(Artifact.shopApp("2.4.0"), null, NAME, STABLE));
        row(out, 18, "copied by hand", "never meets the gate: lock the other paths");
        section(out, "THE SIGNING KEY IS THE CROWN JEWEL:");
        row(out, 13, "here", "made from a fixed demo seed");
        row(out, 13, "in practice", "in a hardware module; each use approved");
        section(out, "ROTATION IS A PROJECT:");
        line(out, "    every deployment trusts the new key before the old one");
        line(out, "    is revoked: here an overlap of 30 days");
        section(out, "KEYLESS SIGNING (the -with-cosign version):");
        line(out, "    short-lived certificates tied to an identity, in a public log");
    }
}
