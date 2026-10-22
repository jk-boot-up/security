package com.jk.explore.security.localca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.bouncycastle.asn1.x509.CRLReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalCaTest {

    private static final Instant START = LocalCaDemo.START;
    private CertificateAuthority ca;
    private Validator clients;

    @BeforeEach
    void setUp() {
        ca = new CertificateAuthority(START, LocalCaDemo.ALLOWED);
        clients = new Validator(ca.root());
    }

    private X509Certificate issue(String requester, String name, Profile profile, Duration lifetime) throws RefusedException {
        return ca.issue(Csr.create(Keys.newPair(), List.of(name)), requester, profile, lifetime, START);
    }

    private List<X509Certificate> chain(X509Certificate leaf) {
        return List.of(leaf, ca.issuing());
    }

    @Test
    void issuedCertificateValidatesAgainstTheRoot() throws Exception {
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        assertTrue(clients.check(chain(www), START.plus(Duration.ofDays(1))).accepted());
        assertEquals(-1, www.getBasicConstraints());
        assertEquals(0, ca.issuing().getBasicConstraints());
        assertEquals(1, ca.root().getBasicConstraints());
    }

    @Test
    void certificateIsRefusedBeforeItStartsAndAfterItEnds() throws Exception {
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        assertFalse(clients.check(chain(www), START.minus(Duration.ofDays(1))).accepted());
        assertFalse(clients.check(chain(www), START.plus(Duration.ofDays(91))).accepted());
    }

    @Test
    void policyRefusesBadRequests() {
        assertThrows(RefusedException.class, () -> ca.issue(Csr.create(Keys.newPair(), List.of()), "web-team", Profile.SERVER, Duration.ofDays(90), START));
        assertThrows(RefusedException.class, () -> issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(400)));
        assertThrows(RefusedException.class, () -> issue("payments-team", "pay.example", Profile.SERVER, Duration.ofDays(90)));
        assertThrows(RefusedException.class, () -> issue("web-team", "payments.shop.internal", Profile.SERVER, Duration.ofDays(90)));
        assertThrows(RefusedException.class, () -> issue("stranger", "www.shop.internal", Profile.SERVER, Duration.ofDays(90)));
        assertThrows(RefusedException.class, () -> issue("web-team", "shop.internal", Profile.SERVER, Duration.ofDays(90)));
    }

    @Test
    void shortLivedProfileCapsTheLifetimeAtOneDay() {
        assertThrows(RefusedException.class, () -> issue("cart-team", "cart.shop.internal", Profile.SHORT_LIVED, Duration.ofHours(25)));
    }

    @Test
    void requestMustProvePossessionOfTheKey() {
        KeyPair payments = Keys.newPair();
        KeyPair stranger = Keys.newPair();
        var forged = Csr.signedByAnotherKey(payments.getPublic(), stranger.getPrivate(), List.of("payments.shop.internal"));
        assertFalse(Csr.provesPossession(forged));
        assertThrows(RefusedException.class, () -> ca.issue(forged, "payments-team", Profile.SERVER, Duration.ofDays(90), START));
        assertTrue(Csr.provesPossession(Csr.create(payments, List.of("payments.shop.internal"))));
    }

    @Test
    void issuedCertificateCarriesTheRequestersPublicKey() throws Exception {
        KeyPair keys = Keys.newPair();
        X509Certificate certificate = ca.issue(Csr.create(keys, List.of("payments.shop.internal")), "payments-team", Profile.SERVER, Duration.ofDays(90), START);
        assertEquals(keys.getPublic(), certificate.getPublicKey());
    }

    @Test
    void nameConstraintStopsAnIssuerThatSkipsThePolicy() {
        X509Certificate stray = ca.issueWithoutPolicy(Keys.newPair().getPublic(), "pay.example", START);
        Validator.Result result = clients.check(chain(stray), START.plus(Duration.ofDays(1)));
        assertFalse(result.accepted());
        assertEquals("refused (INVALID_NAME)", result.toString());
    }

    @Test
    void clientProfileGivesClientAuthOnly() throws Exception {
        X509Certificate client = issue("orders-team", "orders.shop.internal", Profile.CLIENT, Duration.ofDays(30));
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), client.getExtendedKeyUsage());
    }

    @Test
    void revokedCertificateIsRefusedOnlyByClientsThatCheck() throws Exception {
        X509Certificate payments = issue("payments-team", "payments.shop.internal", Profile.SERVER, Duration.ofDays(90));
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        X509CRL rootList = ca.rootCrl(START, Duration.ofDays(365), false);
        Instant day10 = START.plus(Duration.ofDays(10));
        ca.revoke(payments.getSerialNumber(), CRLReason.keyCompromise, day10);
        X509CRL list = ca.issuingCrl(day10, Duration.ofDays(7));
        Instant day11 = START.plus(Duration.ofDays(11));
        assertEquals("refused (REVOKED)", clients.checkWithLists(chain(payments), day11, List.of(list, rootList)).toString());
        assertTrue(clients.checkWithLists(chain(www), day11, List.of(list, rootList)).accepted());
        assertTrue(clients.check(chain(payments), day11).accepted());
    }

    @Test
    void anOutOfDateListFailsClosed() throws Exception {
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        X509CRL rootList = ca.rootCrl(START, Duration.ofDays(365), false);
        X509CRL stale = ca.issuingCrl(START.plus(Duration.ofDays(10)), Duration.ofDays(7));
        Instant day20 = START.plus(Duration.ofDays(20));
        assertFalse(clients.checkWithLists(chain(www), day20, List.of(stale, rootList)).accepted());
        X509CRL fresh = ca.issuingCrl(day20, Duration.ofDays(7));
        assertTrue(clients.checkWithLists(chain(www), day20, List.of(fresh, rootList)).accepted());
    }

    @Test
    void revokingTheIssuingAuthorityRevokesEverythingBelowIt() throws Exception {
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        Instant day41 = START.plus(Duration.ofDays(41));
        X509CRL rootList = ca.rootCrl(START.plus(Duration.ofDays(40)), Duration.ofDays(365), true);
        X509CRL list = ca.issuingCrl(START.plus(Duration.ofDays(40)), Duration.ofDays(7));
        assertEquals("refused (REVOKED)", clients.checkWithLists(chain(www), day41, List.of(list, rootList)).toString());
    }

    @Test
    void shortLivedCertificateRenewsAtTwoThirdsWithANewKey() throws Exception {
        ShortLived cart = new ShortLived(ca, "cart-team", "cart.shop.internal");
        int failed = 0;
        java.security.PublicKey firstKey = null;
        for (int hour = 0; hour < 72; hour++) {
            Instant now = START.plus(Duration.ofHours(hour));
            cart.tick(now);
            if (firstKey == null) {
                firstKey = cart.current().getPublicKey();
            }
            if (!clients.check(chain(cart.current()), now).accepted()) {
                failed++;
            }
        }
        assertEquals(4, cart.renewals());
        assertEquals(0, failed);
        assertFalse(firstKey.equals(cart.current().getPublicKey()));
    }

    @Test
    void whenTheRenewalJobStopsTheCertificateJustExpires() throws Exception {
        ShortLived cart = new ShortLived(ca, "cart-team", "cart.shop.internal");
        cart.tick(START);
        cart.stopJob();
        assertFalse(cart.tick(START.plus(Duration.ofHours(20))));
        assertTrue(clients.check(chain(cart.current()), START.plus(Duration.ofHours(23))).accepted());
        assertEquals("refused (EXPIRED)", clients.check(chain(cart.current()), START.plus(Duration.ofHours(25))).toString());
    }

    @Test
    void inventoryFlagsCertificatesNearingOrPastExpiry() throws Exception {
        X509Certificate www = issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        assertEquals(Inventory.Status.FINE, Inventory.status(www, START.plus(Duration.ofDays(30))));
        assertEquals(Inventory.Status.RENEW_NOW, Inventory.status(www, START.plus(Duration.ofDays(65))));
        assertEquals(Inventory.Status.EXPIRED, Inventory.status(www, START.plus(Duration.ofDays(90))));
    }

    @Test
    void rootKeyIsUsedRarelyAndEveryDecisionIsAudited() throws Exception {
        assertEquals(2, ca.rootSignatures());
        issue("web-team", "www.shop.internal", Profile.SERVER, Duration.ofDays(90));
        assertEquals(2, ca.rootSignatures());
        assertThrows(RefusedException.class, () -> issue("web-team", "pay.example", Profile.SERVER, Duration.ofDays(90)));
        assertEquals(3, ca.audit().size());
        assertTrue(ca.audit().get(2).startsWith("refused web-team"));
    }
}
