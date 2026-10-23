package com.jk.explore.security.securepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerifierTest {

    private static final String COMMIT = "a".repeat(40);
    private static final String TREE = "b".repeat(40);

    @TempDir
    Path dir;

    private JdkSignatures.Pair trusted;
    private JdkSignatures.Pair attacker;
    private Bundle bundle;
    private final Verifier.Policy policy = new Verifier.Policy(Provenance.BUILDER, COMMIT, TREE);
    private final JdkSignatures signatures = new JdkSignatures();

    @BeforeEach
    void release() throws Exception {
        trusted = JdkSignatures.newPair(dir, "trusted");
        attacker = JdkSignatures.newPair(dir, "attacker");
        bundle = make(dir.resolve("good"), "jar bytes", COMMIT, TREE, trusted);
    }

    private Bundle make(Path where, String jar, String commit, String tree, JdkSignatures.Pair signer) throws Exception {
        Files.createDirectories(where);
        Bundle b = new Bundle(where);
        Files.writeString(b.jar(), jar);
        Files.writeString(b.provenance(), Provenance.create(new Provenance.Facts(Bundle.JAR, Digest.sha256(jar.getBytes(StandardCharsets.UTF_8)), commit, tree,
                List.of(new Sbom.Component("commons-codec", "commons-codec", "1.17.1")), Digest.sha256(new byte[0]))));
        JdkSignatures.sign(signer.signing(), b.jar(), b.jarSignature());
        JdkSignatures.sign(signer.signing(), b.provenance(), b.provenanceSignature());
        return b;
    }

    @Test
    void aGenuineReleaseVerifies() {
        assertTrue(Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).ok());
    }

    @Test
    void aChangedJarFailsOnItsSignature() throws Exception {
        Files.writeString(bundle.jar(), "jar bytes!");
        assertEquals("artifact signature", Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void aReleaseSignedByAnotherKeyFails() throws Exception {
        Bundle forged = make(dir.resolve("forged"), "jar bytes", COMMIT, TREE, attacker);
        assertEquals("artifact signature", Verifier.verify(forged, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void aMissingSignatureFails() throws Exception {
        Files.delete(bundle.jarSignature());
        assertEquals("files present", Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void aChangedProvenanceFailsOnItsSignature() throws Exception {
        Files.writeString(bundle.provenance(), Files.readString(bundle.provenance()).replace(COMMIT, "c".repeat(40)));
        assertEquals("provenance signature", Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void provenanceOfAnotherJarFailsOnTheDigestEvenWhenBothSignaturesAreGenuine() throws Exception {
        Bundle other = make(dir.resolve("other"), "other jar", COMMIT, TREE, trusted);
        Files.copy(other.provenance(), bundle.provenance(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(other.provenanceSignature(), bundle.provenanceSignature(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertEquals("artifact digest", Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void anOldGenuineReleaseFailsOnTheSourceCommit() throws Exception {
        Bundle old = make(dir.resolve("old"), "old jar", "d".repeat(40), TREE, trusted);
        assertEquals("source commit", Verifier.verify(old, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void aReleaseFromAnotherTreeFails() throws Exception {
        Bundle other = make(dir.resolve("tree"), "jar bytes", COMMIT, "e".repeat(40), trusted);
        assertEquals("source tree", Verifier.verify(other, trusted.publicKeyFile(), policy, signatures).failed());
    }

    @Test
    void anUntrustedBuilderFails() {
        Verifier.Policy other = new Verifier.Policy("https://elsewhere.example/builder", COMMIT, TREE);
        assertEquals("builder", Verifier.verify(bundle, trusted.publicKeyFile(), other, signatures).failed());
    }

    @Test
    void provenanceThatIsNotJsonFailsClosed() throws Exception {
        Files.writeString(bundle.provenance(), "not json");
        JdkSignatures.sign(trusted.signing(), bundle.provenance(), bundle.provenanceSignature());
        assertFalse(Verifier.verify(bundle, trusted.publicKeyFile(), policy, signatures).ok());
    }

    @Test
    void deployCopiesOnlyAGenuineRelease() throws Exception {
        Deploy deploy = new Deploy(dir.resolve("production"), trusted.publicKeyFile(), policy, signatures);
        Files.writeString(bundle.jar(), "jar bytes!");
        assertFalse(deploy.deploy(bundle).ok());
        assertEquals(0, deploy.deployed());
        Bundle good = make(dir.resolve("again"), "jar bytes", COMMIT, TREE, trusted);
        assertTrue(deploy.deploy(good).ok());
        assertEquals(1, deploy.deployed());
    }
}
