package com.jk.explore.security.tlsaudit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Real servers and the real testssl.sh. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TlsAuditTest {

    private static final String HOST = "www.shop.localhost";

    private Nginx good;
    private Nginx copied;
    private Nginx expired;
    private Nginx wrong;
    private LegacyServer legacy;
    private Report goodReport;
    private Report copiedReport;
    private Report legacyReport;
    private Report expiredReport;
    private Report wrongReport;
    private Report addressReport;

    @BeforeAll
    void start() throws Exception {
        Path work = Files.createTempDirectory("tlsaudit-test");
        LocalCa ca = new LocalCa(work.resolve("ca"));
        ca.issue("good", "-1h", "2160h", false, HOST);
        ca.issue("legacy", "-1h", "2160h", true, HOST);
        ca.issue("expired", "-72h", "-24h", false, HOST);
        ca.issue("wrong", "-1h", "2160h", false, "pay.shop.localhost");
        TestSsl tool = new TestSsl(ca.root(), work.resolve("scans"));
        good = new Nginx(work.resolve("good"));
        copied = new Nginx(work.resolve("copied"));
        expired = new Nginx(work.resolve("expired"));
        wrong = new Nginx(work.resolve("wrong"));
        legacy = new LegacyServer(ca.pkcs12("legacy"));
        good.start(Nginx.Settings.guided(), ca.cert("good").toString(), ca.key("good").toString());
        copied.start(Nginx.Settings.copied(), ca.cert("good").toString(), ca.key("good").toString());
        expired.start(Nginx.Settings.guided(), ca.cert("expired").toString(), ca.key("expired").toString());
        wrong.start(Nginx.Settings.guided(), ca.cert("wrong").toString(), ca.key("wrong").toString());
        var a = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, good.port(), false));
        var b = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, copied.port(), false));
        var c = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, legacy.port(), false));
        var d = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, expired.port(), false));
        var e = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, wrong.port(), false));
        var f = java.util.concurrent.CompletableFuture.supplyAsync(() -> run(tool, HOST, good.port(), true));
        goodReport = a.join();
        copiedReport = b.join();
        legacyReport = c.join();
        expiredReport = d.join();
        wrongReport = e.join();
        addressReport = f.join();
    }

    private static Report run(TestSsl tool, String name, int port, boolean byAddress) {
        try {
            return byAddress ? tool.scanByAddress(port) : tool.scan(name, port);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    void stop() throws Exception {
        good.close();
        copied.close();
        expired.close();
        wrong.close();
        legacy.close();
    }

    @Test
    void theGuidedServerOffersOnlyModernProtocolsAndPassesTheGate() {
        assertTrue(goodReport.offered("TLS1_2") && goodReport.offered("TLS1_3"));
        assertFalse(goodReport.offered("TLS1") || goodReport.offered("TLS1_1"));
        assertEquals("pass", Policy.verdict(goodReport));
        assertEquals(List.of("cert_revocation"), goodReport.idsAt("HIGH", "CRITICAL"));
    }

    @Test
    void theCopiedConfigurationBreaksFourRulesThatNoBrowserShows() {
        assertEquals(List.of("obsolete-suites", "hsts", "headers", "banner"), Policy.failures(copiedReport));
    }

    @Test
    void theLegacyServerOffersTlsOneZeroAndNoForwardSecrecy() {
        assertTrue(legacyReport.offered("TLS1") && legacyReport.offered("TLS1_1"));
        assertEquals("LOW", legacyReport.severity("TLS1"));
        assertTrue(Policy.failures(legacyReport).containsAll(List.of("protocols", "tls13", "forward-secrecy")));
    }

    @Test
    void anExpiredCertificateIsCritical() {
        assertEquals("CRITICAL", expiredReport.severity("cert_expirationStatus"));
        assertTrue(Policy.failures(expiredReport).contains("expiry"));
        assertTrue(Policy.failures(expiredReport).contains("chain"));
    }

    @Test
    void aCertificateForAnotherNameIsHigh() {
        assertEquals("HIGH", wrongReport.severity("cert_trust"));
        assertEquals(List.of("name"), Policy.failures(wrongReport));
    }

    @Test
    void scanningByAddressReportsAFalseNameProblem() {
        assertEquals("HIGH", addressReport.severity("cert_trust"));
        assertEquals(List.of("name"), Policy.failures(addressReport));
    }
}
