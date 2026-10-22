package com.jk.explore.security.cosignsigning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real registry in Docker and the real cosign command. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CosignSigningTest {

    private Lab lab;
    private Cosign cosign;

    @BeforeAll
    void start() throws Exception {
        lab = Lab.start();
        cosign = new Cosign(lab.keys());
        for (String key : List.of("release", "attacker")) {
            cosign.generateKeyPair(key);
        }
    }

    @AfterAll
    void stop() {
        lab.close();
    }

    @Test
    void aTagCanBeMovedToAnotherImage() throws Exception {
        String release = lab.push(Lab.RELEASE_IMAGE, "tags/app", "1");
        String other = lab.push(Lab.ATTACKER_IMAGE, "tags/app", "1");
        assertNotEquals(release, other);
        assertEquals(other, lab.resolve("tags/app", "1"));
    }

    @Test
    void aSignedDigestVerifiesWithTheRightKeyOnly() throws Exception {
        String digest = lab.push(Lab.RELEASE_IMAGE, "signed/app", "1");
        assertTrue(cosign.sign("release", digest, Map.of()).ok());
        assertTrue(cosign.verify("release", digest, Map.of()).ok());
        assertEquals("refused (no signature that this key made)", cosign.verify("attacker", digest, Map.of()).reason());
    }

    @Test
    void anUnsignedDigestIsRefused() throws Exception {
        String digest = lab.push(Lab.UNSIGNED_IMAGE, "unsigned/app", "1");
        assertEquals("refused (no signature for this digest)", cosign.verify("release", digest, Map.of()).reason());
    }

    @Test
    void aMovedTagLosesItsSignature() throws Exception {
        String digest = lab.push(Lab.RELEASE_IMAGE, "moved/app", "1");
        cosign.sign("release", digest, Map.of());
        assertEquals("accepted (signed by the release key)", CosignSigningDemo.gate(lab, cosign, "1", List.of("release"), Map.of(), "moved/app"));
        lab.push(Lab.ATTACKER_IMAGE, "moved/app", "1");
        assertEquals("refused (no signature for this digest)", CosignSigningDemo.gate(lab, cosign, "1", List.of("release"), Map.of(), "moved/app"));
    }

    @Test
    void signedClaimsMustMatchWhatTheGateRequires() throws Exception {
        String digest = lab.push(Lab.BETA_IMAGE, "claims/app", "1");
        cosign.sign("release", digest, Map.of("channel", "beta"));
        assertTrue(cosign.verify("release", digest, Map.of("channel", "beta")).ok());
        assertEquals("refused (the signed claims do not say what the gate requires)", cosign.verify("release", digest, Map.of("channel", "stable")).reason());
    }

    @Test
    void theSignatureIsStoredInTheRegistryNextToTheImage() throws Exception {
        String digest = lab.push(Lab.THIEF_IMAGE, "stored/app", "1");
        assertFalse(lab.tags("stored/app").contains("sha256-"));
        cosign.sign("release", digest, Map.of());
        assertTrue(lab.tags("stored/app").contains("sha256-"));
    }

    @Test
    void anAttestationCarriesAPredicateThatTheGateCanRead() throws Exception {
        String digest = lab.push(Lab.LAPTOP_IMAGE, "attested/app", "1");
        Path predicate = Files.writeString(lab.keys().resolve("p.json"), "{\"builder\":{\"id\":\"https://ci.shop.example\"}}");
        assertTrue(cosign.attest("release", digest, "slsaprovenance", predicate).ok());
        Cosign.Result r = cosign.verifyAttestation("release", digest, "slsaprovenance");
        assertTrue(r.ok());
        assertEquals("https://ci.shop.example", ((Map<?, ?>) Cosign.predicateOf(r.output()).get("builder")).get("id"));
        assertFalse(cosign.verifyAttestation("attacker", digest, "slsaprovenance").ok());
    }

    @Test
    void aWrongPassphraseCannotSign() throws Exception {
        String digest = lab.push(Lab.UNSIGNED_IMAGE, "pass/app", "1");
        assertFalse(cosign.sign("release", "wrong", digest, Map.of()).ok());
    }
}
