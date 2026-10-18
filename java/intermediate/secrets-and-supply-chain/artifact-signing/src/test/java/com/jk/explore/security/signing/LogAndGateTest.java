package com.jk.explore.security.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LogAndGateTest {

    private final SigningKey stable = SigningKey.demo("stable-2026a", "stable");
    private final Artifact jar = Artifact.shopApp("2.4.0");
    private final SignedRelease release = stable.sign(Statement.of(jar, "shop-app", "2.4.0", "stable", 120));

    @Test
    void theTrustingGateDeploysAnything() {
        assertTrue(DeployGate.trusting(jar.withByteChanged(1)).accepted());
    }

    @Test
    void aLoggedReleaseIsAcceptedAndAnUnloggedOneIsNot() {
        TransparencyLog log = TransparencyLog.demo();
        DeployGate gate = new DeployGate(new KeyRing().trust(stable, 0), log);
        assertEquals("the signature is not in the public log", gate.checkWithLog(jar, release, "shop-app", "stable").reason());
        log.append(release, 120);
        assertTrue(gate.checkWithLog(jar, release, "shop-app", "stable").accepted());
        assertFalse(gate.checkWithLog(jar, null, "shop-app", "stable").accepted());
    }

    @Test
    void revocationUsesTheLogsDaySoBackdatingFails() {
        TransparencyLog log = TransparencyLog.demo();
        KeyRing ring = new KeyRing().trust(stable, 0);
        DeployGate gate = new DeployGate(ring, log);
        Artifact other = jar.withByteChanged(5);
        SignedRelease backdated = stable.sign(Statement.of(other, "shop-app", "2.4.0", "stable", 120));
        log.append(backdated, 150);
        ring.revoke("stable-2026a", 130);
        assertTrue(new Verifier(ring).verify(other, backdated, "shop-app", "stable").accepted());
        assertEquals("logged on day 150, after key stable-2026a was revoked on day 130", gate.checkWithLog(other, backdated, "shop-app", "stable").reason());
    }

    @Test
    void headsAreSignedByTheLog() {
        TransparencyLog log = TransparencyLog.demo();
        log.append(release, 120);
        TransparencyLog.TreeHead head = log.head();
        assertTrue(TransparencyLog.headIsSigned(head, log.publicKey()));
        TransparencyLog.TreeHead forged = new TransparencyLog.TreeHead(head.size() + 1, head.root(), head.signature());
        assertFalse(TransparencyLog.headIsSigned(forged, log.publicKey()));
    }

    @Test
    void theMonitorReportsOnlyUnknownReleasesSignedWithStoreKeys() {
        KeyRing ring = new KeyRing().trust(stable, 0);
        TransparencyLog log = TransparencyLog.demo();
        log.append(release, 120);
        Artifact other = jar.withByteChanged(5);
        log.append(stable.sign(Statement.of(other, "shop-app", "2.4.0", "stable", 120)), 150);
        log.append(SigningKey.demo("someone-else", "stable").sign(Statement.of(other, "their-app", "1.0", "stable", 150)), 150);
        Monitor monitor = new Monitor(ring, Set.of(Digests.hex(jar.sha256())));
        List<String> findings = monitor.unexpected(log);
        assertEquals(1, findings.size());
        assertTrue(findings.getFirst().startsWith("entry 1: version 2.4.0 signed with stable-2026a"));
    }

    @Test
    void aForkedLogShowsTwoHeadsOfOneSize() {
        TransparencyLog log = TransparencyLog.demo();
        log.append(release, 120);
        TransparencyLog.TreeHead earlier = log.head();
        TransparencyLog other = log.fork();
        log.append(stable.sign(Statement.of(Artifact.shopApp("2.4.1"), "shop-app", "2.4.1", "stable", 121)), 121);
        other.append(stable.sign(Statement.of(jar.withByteChanged(3), "shop-app", "2.4.0", "stable", 121)), 121);
        assertTrue(TransparencyLog.consistent(earlier, log.head(), log.consistencyProof(1, 2)));
        assertTrue(TransparencyLog.consistent(earlier, other.head(), other.consistencyProof(1, 2)));
        assertEquals(log.head().size(), other.head().size());
        assertFalse(Digests.same(log.head().root(), other.head().root()));
        assertFalse(TransparencyLog.consistent(log.head(), other.head(), List.of()));
    }
}
