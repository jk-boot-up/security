package com.jk.explore.security.stepca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real step-ca, the real step command, and a client written with the JDK. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StepCaTest {

    private StepCa ca;
    private Path work;

    @BeforeAll
    void start() throws Exception {
        ca = new StepCa();
        work = Files.createTempDirectory("stepca-test");
    }

    @AfterAll
    void stop() {
        ca.close();
    }

    @Test
    void theIssuingAuthorityIsSignedByTheRootAndCannotCreateAuthorities() throws Exception {
        X509Certificate root = Pki.first(ca.root());
        X509Certificate issuing = Pki.first(ca.intermediate());
        issuing.verify(root.getPublicKey());
        assertEquals(0, issuing.getBasicConstraints());
        assertTrue(Files.readString(ca.rootKey()).contains("ENCRYPTED"));
    }

    @Test
    void aClientWithoutTheRootCannotTalkToTheCa() {
        org.junit.jupiter.api.Assertions.assertThrows(javax.net.ssl.SSLHandshakeException.class,
                () -> java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(ca.url() + "/health")).build(), java.net.http.HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void aTokenGivesADayLongCertificateThatTheRootValidates() throws Exception {
        assertTrue(ca.issueWithToken(work, "a", ca.token("a.shop.localhost"), "a.shop.localhost").ok());
        X509Certificate cert = Pki.first(work.resolve("a.crt"));
        assertEquals(24, Duration.between(cert.getNotBefore().toInstant(), cert.getNotAfter().toInstant()).toHours());
        assertEquals("accepted", Pki.validate(work.resolve("a.crt"), ca.root(), null, false, new Date()));
    }

    @Test
    void aTokenWorksOnceAndOnlyForItsName() throws Exception {
        String token = ca.token("b.shop.localhost");
        assertTrue(ca.issueWithToken(work, "b", token, "b.shop.localhost").ok());
        assertFalse(ca.issueWithToken(work, "b2", token, "b.shop.localhost").ok());
        assertFalse(ca.issueWithToken(work, "b3", ca.token("x.shop.localhost"), "b.shop.localhost").ok());
    }

    @Test
    void theCaRefusesALongerLifetimeAndANameOutsideItsPolicy() throws Exception {
        assertFalse(ca.issueWithToken(work, "c", ca.token("c.shop.localhost"), "c.shop.localhost", "--not-after", "2000h").ok());
        assertTrue(ca.issueWithToken(work, "p", ca.token("pay.example"), "pay.example").reason().contains("policy"));
    }

    @Test
    void acmeIssuesForAnAllowedNameAndRefusesAForeignOne() throws Exception {
        assertTrue(ca.issueWithAcme(work, "d", "d.shop.localhost").ok());
        assertEquals("accepted", Pki.validate(work.resolve("d.crt"), ca.root(), null, false, new Date()));
        assertFalse(ca.issueWithAcme(work, "e", "evil.example").ok());
    }

    @Test
    void renewalGivesANewSerialAndRekeyingANewKey() throws Exception {
        assertTrue(ca.issueWithToken(work, "r", ca.token("r.shop.localhost"), "r.shop.localhost").ok());
        X509Certificate first = Pki.first(work.resolve("r.crt"));
        Thread.sleep(1100);
        assertTrue(ca.renew(work.resolve("r.crt"), work.resolve("r.key")).ok());
        X509Certificate second = Pki.first(work.resolve("r.crt"));
        assertNotEquals(first.getSerialNumber(), second.getSerialNumber());
        assertEquals(first.getPublicKey(), second.getPublicKey());
        assertTrue(ca.rekey(work.resolve("r.crt"), work.resolve("r.key")).ok());
        assertNotEquals(first.getPublicKey(), Pki.first(work.resolve("r.crt")).getPublicKey());
    }

    @Test
    void anotherKeyCannotRenew() throws Exception {
        assertTrue(ca.issueWithToken(work, "k1", ca.token("k1.shop.localhost"), "k1.shop.localhost").ok());
        assertTrue(ca.issueWithToken(work, "k2", ca.token("k2.shop.localhost"), "k2.shop.localhost").ok());
        assertFalse(ca.renew(work.resolve("k1.crt"), work.resolve("k2.key")).ok());
    }

    @Test
    void revocationStopsRenewalAndShowsInTheListButOnlyForClientsThatCheck() throws Exception {
        assertTrue(ca.issueWithToken(work, "v", ca.token("v.shop.localhost"), "v.shop.localhost").ok());
        X509CRL before = Pki.crl(ca.crl());
        assertEquals("accepted", Pki.validate(work.resolve("v.crt"), ca.root(), before, true, new Date()));
        assertTrue(ca.revoke(work.resolve("v.crt"), work.resolve("v.key")).ok());
        assertFalse(ca.renew(work.resolve("v.crt"), work.resolve("v.key")).ok());
        X509CRL after = Pki.crl(ca.crl());
        assertEquals("refused (REVOKED)", Pki.validate(work.resolve("v.crt"), ca.root(), after, true, new Date()));
        assertEquals("accepted", Pki.validate(work.resolve("v.crt"), ca.root(), null, false, new Date()));
    }

    @Test
    void aClientThatWantsToCheckRefusesWithoutAListOrWithAStaleOne() throws Exception {
        assertTrue(ca.issueWithToken(work, "s", ca.token("s.shop.localhost"), "s.shop.localhost").ok());
        assertEquals("refused (UNDETERMINED_REVOCATION_STATUS)", Pki.validate(work.resolve("s.crt"), ca.root(), null, true, new Date()));
        X509CRL crl = Pki.crl(ca.crl());
        Date later = Date.from(crl.getNextUpdate().toInstant().plus(Duration.ofHours(1)));
        assertEquals("refused (UNDETERMINED_REVOCATION_STATUS)", Pki.validate(work.resolve("s.crt"), ca.root(), crl, true, later));
    }
}
