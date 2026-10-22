package com.jk.explore.security.tlsservers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real nginx and a real Caddy on loopback ports. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TlsServersTest {

    private static final String HOST = "www.shop.localhost";

    private LocalCa ca;
    private Nginx nginx;
    private Caddy caddy;
    private SSLContext trust;

    @BeforeAll
    void start() throws Exception {
        Path work = Files.createTempDirectory("tlsservers-test");
        ca = new LocalCa(work.resolve("ca"));
        ca.issue("www", "24h", HOST);
        nginx = new Nginx(work.resolve("nginx"), ca.cert("www").toString(), ca.key("www").toString(), ca.root().toString());
        caddy = new Caddy(work.resolve("caddy"));
        trust = Https.trusting(ca.root());
    }

    @AfterAll
    void stop() throws Exception {
        nginx.close();
        caddy.close();
    }

    @Test
    void nginxServesACertificateThatTheLabRootValidatesAndTheDefaultRootsDoNot() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        assertEquals(200, Https.get(nginx.httpsPort(), HOST, "/", trust).status());
        assertTrue(Https.get(nginx.httpsPort(), HOST, "/", SSLContext.getDefault()).refused());
        assertEquals("the certificate is not for that name", Https.get(nginx.httpsPort(), "pay.shop.localhost", "/", trust).refusal());
    }

    @Test
    void plainHttpIsSentOnToHttps() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        Https.Response r = Https.plain(nginx.httpPort(), HOST, "/cart");
        assertEquals(301, r.status());
        assertTrue(r.header("location").startsWith("https://" + HOST));
    }

    @Test
    void serverTokensOffHidesTheVersion() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        assertTrue(Https.get(nginx.httpsPort(), HOST, "/", trust).header("server").startsWith("nginx/"));
        nginx.restart(Nginx.Settings.defaults().serverTokens(false));
        assertEquals("nginx", Https.get(nginx.httpsPort(), HOST, "/", trust).header("server"));
    }

    @Test
    void aTlsOneThreeOnlyServerRefusesAClientThatOffersOnlyOneTwo() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        assertEquals("TLSv1.2", Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.2").protocol());
        nginx.restart(Nginx.Settings.defaults().protocols("TLSv1.3"));
        assertEquals("no protocol version in common", Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.2").refusal());
        assertEquals("TLSv1.3", Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.3").protocol());
    }

    @Test
    void hstsIsSentOverHttpsOnlyWhenConfiguredAndNeverOverPlainHttp() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        assertNull(Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security"));
        nginx.restart(Nginx.Settings.defaults().hsts(true));
        assertNotNull(Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security"));
        assertNull(Https.plain(nginx.httpPort(), HOST, "/").header("strict-transport-security"));
    }

    @Test
    void aLocationWithItsOwnAddHeaderLosesTheServersHsts() throws Exception {
        nginx.restart(Nginx.Settings.defaults().hsts(true).accountHeader(true, false));
        Https.Response r = Https.get(nginx.httpsPort(), HOST, "/account", trust);
        assertEquals("DENY", r.header("x-frame-options"));
        assertNull(r.header("strict-transport-security"));
        nginx.restart(Nginx.Settings.defaults().hsts(true).accountHeader(true, true));
        assertNotNull(Https.get(nginx.httpsPort(), HOST, "/account", trust).header("strict-transport-security"));
    }

    @Test
    void stapledAnswersNeedAResponderInTheCertificate() throws Exception {
        nginx.restart(Nginx.Settings.defaults().stapling(true));
        assertEquals(0, Https.get(nginx.httpsPort(), HOST, "/", trust).stapled());
        assertTrue(nginx.errorLog().contains("no OCSP responder URL"));
    }

    @Test
    void nginxServesTheOldCertificateUntilItIsReloaded() throws Exception {
        nginx.restart(Nginx.Settings.defaults());
        var before = Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber();
        ca.issue("www", "24h", HOST);
        assertEquals(before, Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber());
        nginx.reload();
        assertNotEquals(before, Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber());
    }

    @Test
    void caddyMakesItsOwnAuthorityAndShortCertificates() throws Exception {
        caddy.start(false);
        SSLContext caddyTrust = Https.trusting(caddy.root());
        Https.Response r = Https.get(caddy.httpsPort(), HOST, "/", caddyTrust);
        assertEquals(200, r.status());
        assertEquals(2, r.chain().size());
        assertTrue(r.leaf().getIssuerX500Principal().getName().contains("Caddy Local Authority"));
        assertEquals(12, java.time.Duration.between(r.leaf().getNotBefore().toInstant(), r.leaf().getNotAfter().toInstant()).toHours());
        assertTrue(Https.get(caddy.httpsPort(), HOST, "/", trust).refused());
    }

    @Test
    void caddyRedirectsAndHidesItsVersionAndSendsHstsOnlyWhenAsked() throws Exception {
        caddy.start(false);
        SSLContext caddyTrust = Https.trusting(caddy.root());
        assertEquals(308, Https.plain(caddy.httpPort(), HOST, "/").status());
        Https.Response r = Https.get(caddy.httpsPort(), HOST, "/", caddyTrust);
        assertEquals("Caddy", r.header("server"));
        assertNull(r.header("strict-transport-security"));
        assertEquals("TLSv1.2", Https.get(caddy.httpsPort(), HOST, "/", caddyTrust, "TLSv1.2").protocol());
        caddy.start(true);
        assertNotNull(Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(caddy.root())).header("strict-transport-security"));
        assertFalse(Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(caddy.root())).refused());
    }
}
