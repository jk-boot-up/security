package com.jk.explore.security.pinning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PinningTest {

    private static final Instant NOW = PinningDemo.NOW;
    private static final String HOST = PinningDemo.HOST;
    private static Pki.Identity root;
    private static Pki.Identity issuer;
    private static KeyPair key;
    private static X509Certificate leaf;
    private static List<X509Certificate> chain;
    private static Pki.Identity rogue;
    private static Pki.Identity rogueIssuer;
    private static List<X509Certificate> interception;

    @BeforeAll
    static void setUp() {
        root = Pki.root("Root", NOW);
        issuer = Pki.intermediate("Issuer", root, NOW);
        key = Pki.newKey();
        leaf = Pki.leaf(HOST, issuer, key, NOW, Duration.ofDays(90), List.of(HOST));
        chain = List.of(leaf, issuer.certificate());
        rogue = Pki.root("Rogue", NOW);
        rogueIssuer = Pki.intermediate("RogueIssuer", rogue, NOW);
        interception = List.of(Pki.leaf(HOST, rogueIssuer, Pki.newKey(), NOW, Duration.ofDays(1), List.of(HOST)), rogueIssuer.certificate());
    }

    private static Set<X509Certificate> device(boolean withRogue) {
        return withRogue ? Set.of(root.certificate(), rogue.certificate()) : Set.of(root.certificate());
    }

    @Test
    void pinsAreHashesOfThePublicKeyInfo() {
        String pin = Spki.pin(leaf);
        assertEquals(44, pin.length());
        assertEquals(pin, Spki.pin(key.getPublic()));
        assertFalse(pin.equals(Spki.pin(issuer.certificate())));
    }

    @Test
    void withoutPinsAnExtraRootOnTheDeviceLetsAnInterceptorIn() {
        assertEquals(Outcome.ACCEPTED, new PinnedClient(device(true), null).connect(interception, HOST, NOW));
        assertEquals(Outcome.REFUSED_BY_VALIDATION, new PinnedClient(device(false), null).connect(interception, HOST, NOW));
    }

    @Test
    void aPinStopsAnInterceptorWhoseRootTheDeviceTrusts() {
        PinnedClient c = new PinnedClient(device(true), PinPolicy.of(Spki.pin(leaf)));
        assertEquals(Outcome.ACCEPTED, c.connect(chain, HOST, NOW));
        assertEquals(Outcome.REFUSED_BY_PIN, c.connect(interception, HOST, NOW));
        assertEquals(1, c.reports().size());
    }

    @Test
    void validationComesFirstSoAPinCannotMakeABadChainGood() {
        PinnedClient c = new PinnedClient(device(false), PinPolicy.of(Spki.pin(interception.get(0))));
        assertEquals(Outcome.REFUSED_BY_VALIDATION, c.connect(interception, HOST, NOW));
        assertEquals(Outcome.REFUSED_BY_VALIDATION, new PinnedClient(device(false), null).connect(chain, "other.example", NOW));
        assertEquals(Outcome.REFUSED_BY_VALIDATION, new PinnedClient(device(false), null).connect(chain, HOST, NOW.plus(Duration.ofDays(120))));
    }

    @Test
    void renewingWithANewKeyBreaksALeafPinButNotAnIssuerPin() {
        Instant later = NOW.plus(Duration.ofDays(80));
        List<X509Certificate> renewed = List.of(Pki.leaf(HOST, issuer, Pki.newKey(), later, Duration.ofDays(90), List.of(HOST)), issuer.certificate());
        assertEquals(Outcome.REFUSED_BY_PIN, new PinnedClient(device(false), PinPolicy.of(Spki.pin(leaf))).connect(renewed, HOST, later));
        assertEquals(Outcome.ACCEPTED, new PinnedClient(device(false), PinPolicy.of(Spki.pin(issuer.certificate()))).connect(renewed, HOST, later));
    }

    @Test
    void aBackupPinLetsTheServerSwitchKeysWithoutAnAppUpdate() {
        KeyPair backup = Pki.newKey();
        Instant later = NOW.plus(Duration.ofDays(80));
        List<X509Certificate> switched = List.of(Pki.leaf(HOST, issuer, backup, later, Duration.ofDays(90), List.of(HOST)), issuer.certificate());
        PinnedClient c = new PinnedClient(device(false), PinPolicy.of(Spki.pin(leaf), Spki.pin(backup.getPublic())));
        assertEquals(Outcome.ACCEPTED, c.connect(switched, HOST, later));
    }

    @Test
    void reportOnlyModeAcceptsAndRecords() {
        PinnedClient c = new PinnedClient(device(true), PinPolicy.of(Spki.pin(leaf)).inReportOnlyMode());
        assertEquals(Outcome.ACCEPTED_REPORT_ONLY, c.connect(interception, HOST, NOW));
        assertEquals(1, c.reports().size());
        assertTrue(c.reports().get(0).startsWith("pin mismatch for " + HOST));
    }

    @Test
    void fleetLockoutIsCountedForTheWholeFleetOnDayZero() {
        Fleet f = new Fleet(10_000, 85);
        assertEquals(10_000, f.lockedOutAtDay0());
        assertEquals(1_500, f.lockedOutAfterAWeek());
    }

    @Test
    void dynamicPinsFromOneResponseBlockTheRealSiteUntilTheyExpire() {
        PinStore store = new PinStore();
        String hdr = "pin-sha256=\"" + Spki.pin(rogueIssuer.certificate()) + "\"; pin-sha256=\"" + Spki.pin(rogue.certificate()) + "\"; max-age=86400";
        assertTrue(store.allows(HOST, chain, NOW), "nothing is pinned yet");
        store.onResponse(HOST, hdr, NOW);
        assertFalse(store.allows(HOST, chain, NOW.plusSeconds(3600)));
        assertTrue(store.allows(HOST, interception, NOW.plusSeconds(3600)));
        assertTrue(store.allows(HOST, chain, NOW.plusSeconds(90_000)));
    }

    @Test
    void dynamicPinsNeedABackupAndMaxAgeZeroClearsThem() {
        PinStore store = new PinStore();
        store.onResponse(HOST, "pin-sha256=\"" + Spki.pin(leaf) + "\"; max-age=86400", NOW);
        assertTrue(store.allows(HOST, interception, NOW), "a single pin is ignored");
        store.onResponse(HOST, "pin-sha256=\"" + Spki.pin(leaf) + "\"; pin-sha256=\"x\"; max-age=86400", NOW);
        assertFalse(store.allows(HOST, interception, NOW));
        store.onResponse(HOST, "max-age=0", NOW);
        assertTrue(store.allows(HOST, interception, NOW));
    }
}
