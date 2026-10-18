package com.jk.explore.security.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PolicyTest {

    private static final Policy POLICY = ProvenanceDemo.POLICY;
    private static final Builder.Build RELEASE = ProvenanceDemo.CI.build(ProvenanceDemo.main(), "shop.jar", "b1");

    private static String reason(String envelope, byte[] artifact) {
        return POLICY.check(envelope, "shop.jar", artifact).reason();
    }

    @Test
    void theRealReleaseIsDeployed() {
        assertTrue(POLICY.check(RELEASE.provenance(), "shop.jar", RELEASE.artifact()).allowed());
    }

    @Test
    void theStatementNamesTheSourceTheBuilderAndTheArtifact() throws Exception {
        Object statement = Json.read(new String(Dsse.open(RELEASE.provenance(), POLICY.trustedKeys()).payload(), StandardCharsets.UTF_8));
        assertEquals(Provenance.STATEMENT_TYPE, Json.text(statement, "_type"));
        assertEquals(ProvenanceDemo.COMMIT, Json.text(((java.util.List<?>) Json.at(statement, "predicate", "buildDefinition", "resolvedDependencies")).get(0), "digest", "gitCommit"));
        assertEquals(ProvenanceDemo.BUILDER_ID, Json.text(statement, "predicate", "runDetails", "builder", "id"));
        assertEquals(Digests.hex(Digests.sha256(RELEASE.artifact())), Json.text(((java.util.List<?>) Json.at(statement, "subject")).get(0), "digest", "sha256"));
    }

    @Test
    void eachWrongFactIsRefusedWithItsReason() {
        Source fork = new Source("https://git.example.org/mallory/store", ProvenanceDemo.MAIN, ProvenanceDemo.COMMIT, ProvenanceDemo.files("x"));
        Builder.Build forkBuild = ProvenanceDemo.CI.build(fork, "shop.jar", "b2");
        assertEquals("built from another repository", reason(forkBuild.provenance(), forkBuild.artifact()));
        Source branch = new Source(ProvenanceDemo.REPOSITORY, "refs/heads/other", ProvenanceDemo.COMMIT, ProvenanceDemo.files("y"));
        Builder.Build branchBuild = ProvenanceDemo.CI.build(branch, "shop.jar", "b3");
        assertEquals("built from another branch", reason(branchBuild.provenance(), branchBuild.artifact()));
        Builder.Build laptop = Builder.demo("laptop", "laptop-key", 9).build(ProvenanceDemo.main(), "shop.jar", "b4");
        assertEquals("no signature from a trusted builder", reason(laptop.provenance(), laptop.artifact()));
        Builder.Build staging = new Builder("staging", ProvenanceDemo.CI.keyId(), ProvenanceDemo.CI.key()).build(ProvenanceDemo.main(), "shop.jar", "b5");
        assertEquals("built by a builder that is not trusted", reason(staging.provenance(), staging.artifact()));
        assertEquals("the statement is about another artifact", reason(RELEASE.provenance(), forkBuild.artifact()));
        assertEquals("no provenance", reason(null, RELEASE.artifact()));
    }

    @Test
    void aStatementEditedAfterSigningIsRefused() {
        String edited = ProvenanceDemo.editPayload(RELEASE.provenance(), "refs/heads/main", "refs/heads/mainx");
        assertEquals("the signature does not match the statement", reason(edited, RELEASE.artifact()));
    }

    @Test
    void aStatementForAnotherArtifactNameIsRefused() {
        assertFalse(POLICY.check(RELEASE.provenance(), "other.jar", RELEASE.artifact()).allowed());
    }
}
