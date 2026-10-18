package com.jk.explore.security.signing;

import java.util.List;
import java.util.Set;

/** Runs the online store's release story in five numbered acts and prints exact, repeatable results. */
public class SigningDemo {

    static final String NAME = "shop-app";
    static final String STABLE = "stable";

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

    private static Verifier stableVerifier(SigningKey... keys) {
        KeyRing ring = new KeyRing();
        for (SigningKey key : keys) {
            ring.trust(key, 0);
        }
        return new Verifier(ring);
    }

    private static void act1Trusting(StringBuilder out) {
        line(out, "ONE. A deployment that runs whatever arrives.");
        Artifact built = Artifact.shopApp("2.4.0");
        Artifact mirrored = built.withByteChanged(700);
        line(out, "  the release team builds " + built.fileName() + ": " + built.bytes().length + " bytes, sha256 " + Digests.shortHex(built.sha256()));
        line(out, "  the deployment downloads it from a mirror where one byte was changed: sha256 " + Digests.shortHex(mirrored.sha256()));
        line(out, "  the trusting deployment step: " + DeployGate.trusting(mirrored) + "  <- AN ALTERED JAR IS RUNNING");
        line(out, "  a digest published next to the file does not help: whoever can change the file can change the digest beside it");
    }

    private static void act2Signed(StringBuilder out) {
        line(out, "TWO. A signed release, checked before it runs.");
        SigningKey key = SigningKey.demo("stable-2026a", STABLE);
        Artifact jar = Artifact.shopApp("2.4.0");
        Statement statement = Statement.of(jar, NAME, "2.4.0", STABLE, 120);
        SignedRelease release = key.sign(statement);
        Verifier verifier = stableVerifier(key);
        line(out, "  the statement: name=" + statement.name() + " version=" + statement.version() + " channel=" + statement.channel()
                + " sha256=" + statement.sha256().substring(0, 16) + "... signed-day=" + statement.signedDay());
        line(out, "  signed with stable-2026a: an Ed25519 signature of " + release.signature().length + " bytes, the same on every run: "
                + Digests.shortHex(release.signature()) + "...");
        line(out, "  the deployment pins the public key of stable-2026a and checks before it deploys");
        line(out, "  the real jar: " + verifier.verify(jar, release, NAME, STABLE));
        line(out, "  the jar with one byte changed: " + verifier.verify(jar.withByteChanged(700), release, NAME, STABLE));
        Statement edited = new Statement(NAME, "2.4.1", STABLE, statement.sha256(), 120);
        line(out, "  the statement edited to version 2.4.1, signature kept: "
                + verifier.verify(jar, new SignedRelease(edited.bytes(), release.keyId(), release.signature()), NAME, STABLE));
        SigningKey forger = SigningKey.demo("stable-2026a", STABLE, "a forger's own key");
        line(out, "  signed by another key that calls itself stable-2026a: " + verifier.verify(jar, forger.sign(statement), NAME, STABLE));
        SigningKey unknown = SigningKey.demo("attacker-1", STABLE);
        line(out, "  signed by a key the deployment does not know: " + verifier.verify(jar, unknown.sign(statement), NAME, STABLE));
        line(out, "  no signature at all: " + verifier.verify(jar, null, NAME, STABLE));
    }

    private static void act3Keys(StringBuilder out) {
        line(out, "THREE. Signing keys: one per channel, rotated with an overlap, revoked.");
        SigningKey stableA = SigningKey.demo("stable-2026a", STABLE);
        SigningKey stableB = SigningKey.demo("stable-2026b", STABLE);
        SigningKey beta = SigningKey.demo("beta-2026a", "beta");
        KeyRing ring = new KeyRing().trust(stableA, 0).trust(beta, 0);
        Verifier verifier = new Verifier(ring);
        Artifact betaJar = Artifact.shopApp("2.5.0-beta1");
        line(out, "  a beta build signed with beta-2026a, offered to the stable deployment: "
                + verifier.verify(betaJar, beta.sign(Statement.of(betaJar, NAME, "2.5.0-beta1", "beta", 105)), NAME, STABLE));
        ring.trust(stableB, 100);
        line(out, "  day 100: stable-2026b is added, and both stable keys are trusted while the build moves over");
        Artifact jar = Artifact.shopApp("2.4.0");
        line(out, "  signed on day 110 with stable-2026a: " + verifier.verify(jar, stableA.sign(Statement.of(jar, NAME, "2.4.0", STABLE, 110)), NAME, STABLE));
        line(out, "  signed on day 110 with stable-2026b: " + verifier.verify(jar, stableB.sign(Statement.of(jar, NAME, "2.4.0", STABLE, 110)), NAME, STABLE));
        ring.revoke("stable-2026a", 130);
        line(out, "  day 130: stable-2026a is revoked");
        line(out, "  a release it signed on day 120, before the revocation: "
                + verifier.verify(jar, stableA.sign(Statement.of(jar, NAME, "2.4.0", STABLE, 120)), NAME, STABLE));
        line(out, "  a release it signed on day 140: " + verifier.verify(jar, stableA.sign(Statement.of(jar, NAME, "2.4.0", STABLE, 140)), NAME, STABLE));
        Artifact other = Artifact.shopApp("2.4.0").withByteChanged(900);
        line(out, "  but the signer writes the day: a thief with the old key signs a different jar on day 150 and dates it day 120: "
                + verifier.verify(other, stableA.sign(Statement.of(other, NAME, "2.4.0", STABLE, 120)), NAME, STABLE)
                + "  <- A BACKDATED SIGNATURE");
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
        line(out, "  the store logs its releases: 2.3.0 on day 90 and 2.4.0 on day 120; the log's head: size " + seen.size() + ", root "
                + Digests.shortHex(seen.root()) + "..., signed by the log: " + TransparencyLog.headIsSigned(seen, log.publicKey()));
        List<byte[]> proof = log.inclusionProof(1, seen.size());
        line(out, "  the proof that 2.4.0 is in the log: " + proof.size() + " hash, and it checks against the signed head: "
                + MerkleTree.verifyInclusion(MerkleTree.leafHash(r2.logEntry()), 1, seen.size(), proof, seen.root()));
        DeployGate gate = new DeployGate(ring, log);
        line(out, "  the deployment, now requiring a log entry: " + gate.checkWithLog(jar, r2, NAME, STABLE));
        ring.revoke("stable-2026a", 130);
        Artifact other = jar.withByteChanged(900);
        SignedRelease backdated = stableA.sign(Statement.of(other, NAME, "2.4.0", STABLE, 120));
        line(out, "  the thief's backdated jar, not logged: " + gate.checkWithLog(other, backdated, NAME, STABLE));
        TransparencyLog customerView = log.fork();
        TransparencyLog monitorView = log;
        customerView.append(backdated, 150);
        line(out, "  the thief's backdated jar, logged on day 150: "
                + new DeployGate(ring, customerView).checkWithLog(other, backdated, NAME, STABLE) + "  <- THE LOG'S DAY, NOT THE SIGNER'S");
        Monitor monitor = new Monitor(ring, Set.of(Digests.hex(old.sha256()), Digests.hex(jar.sha256())));
        for (String finding : monitor.unexpected(customerView)) {
            line(out, "  the store's monitor reads the log and reports: " + finding);
        }
        Artifact betaJar = Artifact.shopApp("2.5.0-beta1");
        monitorView.append(beta.sign(Statement.of(betaJar, NAME, "2.5.0-beta1", "beta", 140)), 150);
        TransparencyLog.TreeHead shownToMonitor = monitorView.head();
        TransparencyLog.TreeHead shownToCustomer = customerView.head();
        line(out, "  a log operator who hides the entry from the monitor shows two views, both signed by the log, both of size "
                + shownToMonitor.size() + ": roots " + Digests.shortHex(shownToMonitor.root()) + "... and " + Digests.shortHex(shownToCustomer.root()) + "...");
        line(out, "  each view still extends the earlier head of size " + seen.size() + ": "
                + TransparencyLog.consistent(seen, shownToMonitor, monitorView.consistencyProof(seen.size(), shownToMonitor.size())) + " and "
                + TransparencyLog.consistent(seen, shownToCustomer, customerView.consistencyProof(seen.size(), shownToCustomer.size())));
        boolean fork = shownToMonitor.size() == shownToCustomer.size() && !Digests.same(shownToMonitor.root(), shownToCustomer.root());
        line(out, "  but two signed heads of the same size with different roots cannot both be true: the monitor and the customer compare heads, and the fork is proved: "
                + fork);
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        SigningKey key = SigningKey.demo("stable-2026a", STABLE);
        TransparencyLog log = TransparencyLog.demo();
        DeployGate gate = new DeployGate(new KeyRing().trust(key, 0), log);
        Artifact jar = Artifact.shopApp("2.4.0");
        line(out, "  the gate refuses an unsigned jar: " + gate.checkWithLog(jar, null, NAME, STABLE));
        line(out, "  but it protects only the paths that pass through it: a jar copied straight onto a server never meets the gate");
        line(out, "  the signing key is the crown jewel: here it comes from a fixed demo seed; in practice it stays in a hardware module, and each signing is approved and logged");
        line(out, "  rotation is a project: every deployment must trust the new key before the old one is revoked, here an overlap of 30 days");
        line(out, "  keyless signing replaces long-lived keys with short-lived certificates tied to an identity and recorded in a public log: the -with-cosign version");
    }
}
