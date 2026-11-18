package com.jk.explore.security.pinning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The two checks redone by hand agree with the client that uses the JDK's validator. */
class ChainWalkTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final String HOST = "api.bank.example";

    @Test
    void theAnchorIsTheRootThatSignedTheTop() {
        Pki.Identity root = Pki.root("Root A", NOW);
        Pki.Identity other = Pki.root("Root B", NOW);
        Pki.Identity issuer = Pki.intermediate("Issuer", root, NOW);
        List<X509Certificate> chain = List.of(Pki.leaf(HOST, issuer, Pki.newKey(), NOW, Duration.ofDays(90), List.of(HOST)), issuer.certificate());
        assertEquals(root.certificate(), ChainWalk.anchor(chain, Set.of(root.certificate(), other.certificate())).orElseThrow());
        assertTrue(ChainWalk.anchor(chain, Set.of(other.certificate())).isEmpty());
        assertEquals(Outcome.ACCEPTED, new PinnedClient(Set.of(root.certificate()), null).connect(chain, HOST, NOW));
        assertEquals(Outcome.REFUSED_BY_VALIDATION, new PinnedClient(Set.of(other.certificate()), null).connect(chain, HOST, NOW));
    }

    @Test
    void aRootWithTheRightNameButAnotherKeyIsNotTheAnchor() {
        Pki.Identity root = Pki.root("Root A", NOW);
        Pki.Identity impostor = Pki.root("Root A", NOW);
        Pki.Identity issuer = Pki.intermediate("Issuer", root, NOW);
        assertTrue(ChainWalk.anchor(List.of(issuer.certificate()), Set.of(impostor.certificate())).isEmpty());
    }

    @Test
    void thePinnedKeyIsFoundAnywhereInTheChain() {
        Pki.Identity root = Pki.root("Root A", NOW);
        Pki.Identity issuer = Pki.intermediate("Issuer", root, NOW);
        X509Certificate leaf = Pki.leaf(HOST, issuer, Pki.newKey(), NOW, Duration.ofDays(90), List.of(HOST));
        List<X509Certificate> chain = List.of(leaf, issuer.certificate());
        assertEquals(issuer.certificate(), ChainWalk.pinned(chain, Set.of(Spki.pin(issuer.certificate()))).orElseThrow());
        assertEquals(leaf, ChainWalk.pinned(chain, Set.of(Spki.pin(leaf), Spki.pin(issuer.certificate()))).orElseThrow());
        assertTrue(ChainWalk.pinned(chain, Set.of(Spki.pin(root.certificate()))).isEmpty());
    }
}
