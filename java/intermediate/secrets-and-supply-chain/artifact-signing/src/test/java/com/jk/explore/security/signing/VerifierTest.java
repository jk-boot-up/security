package com.jk.explore.security.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VerifierTest {

    private final SigningKey stable = SigningKey.demo("stable-2026a", "stable");
    private final Artifact jar = Artifact.shopApp("2.4.0");
    private final Statement statement = Statement.of(jar, "shop-app", "2.4.0", "stable", 120);

    private Verifier verifier(KeyRing ring) {
        return new Verifier(ring);
    }

    private KeyRing ring() {
        return new KeyRing().trust(stable, 0);
    }

    @Test
    void theStatementHasOneCanonicalForm() {
        assertEquals(statement, Statement.parse(statement.bytes()));
        String text = new String(statement.bytes(), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Statement.parse(text.replace("version=", "Version=").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> Statement.parse((text + "extra=1\n").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> Statement.parse(text.replace("signed-day=120", "signed-day=0120").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> Statement.parse(text.replace(statement.sha256(), "zz").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theRealReleaseIsAccepted() {
        assertTrue(verifier(ring()).verify(jar, stable.sign(statement), "shop-app", "stable").accepted());
    }

    @Test
    void eachKindOfChangeIsRefusedWithItsReason() {
        Verifier verifier = verifier(ring());
        SignedRelease release = stable.sign(statement);
        assertEquals("the artifact does not match the signed digest", verifier.verify(jar.withByteChanged(10), release, "shop-app", "stable").reason());
        Statement edited = new Statement("shop-app", "2.4.1", "stable", statement.sha256(), 120);
        assertEquals("the signature does not match",
                verifier.verify(jar, new SignedRelease(edited.bytes(), release.keyId(), release.signature()), "shop-app", "stable").reason());
        assertEquals("the signature does not match",
                verifier.verify(jar, SigningKey.demo("stable-2026a", "stable", "forger").sign(statement), "shop-app", "stable").reason());
        assertEquals("unknown key x", verifier.verify(jar, SigningKey.demo("x", "stable").sign(statement), "shop-app", "stable").reason());
        assertEquals("no signature", verifier.verify(jar, null, "shop-app", "stable").reason());
        assertEquals("signed for shop-app, not other-app", verifier.verify(jar, release, "other-app", "stable").reason());
        assertEquals("the statement is not a release statement",
                verifier.verify(jar, new SignedRelease("junk".getBytes(StandardCharsets.UTF_8), release.keyId(), release.signature()), "shop-app", "stable").reason());
    }

    @Test
    void aKeyOnlySignsForItsChannel() {
        SigningKey beta = SigningKey.demo("beta-2026a", "beta");
        KeyRing ring = ring().trust(beta, 0);
        SignedRelease betaSigned = beta.sign(Statement.of(jar, "shop-app", "2.4.0", "beta", 120));
        assertFalse(verifier(ring).verify(jar, betaSigned, "shop-app", "stable").accepted());
        assertTrue(verifier(ring).verify(jar, betaSigned, "shop-app", "beta").accepted());
        SignedRelease mislabelled = stable.sign(Statement.of(jar, "shop-app", "2.4.0", "beta", 120));
        assertFalse(verifier(ring).verify(jar, mislabelled, "shop-app", "stable").accepted());
    }

    @Test
    void rotationOverlapsAndRevocationCountsFromItsDay() {
        SigningKey next = SigningKey.demo("stable-2026b", "stable");
        KeyRing ring = ring().trust(next, 100);
        Verifier verifier = verifier(ring);
        assertFalse(verifier.verify(jar, next.sign(Statement.of(jar, "shop-app", "2.4.0", "stable", 99)), "shop-app", "stable").accepted());
        assertTrue(verifier.verify(jar, next.sign(Statement.of(jar, "shop-app", "2.4.0", "stable", 100)), "shop-app", "stable").accepted());
        ring.revoke("stable-2026a", 130);
        assertTrue(verifier.verify(jar, stable.sign(Statement.of(jar, "shop-app", "2.4.0", "stable", 129)), "shop-app", "stable").accepted());
        assertFalse(verifier.verify(jar, stable.sign(Statement.of(jar, "shop-app", "2.4.0", "stable", 130)), "shop-app", "stable").accepted());
    }
}
