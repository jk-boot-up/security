package com.jk.explore.security.tlsaudit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The six JSON files testssl.sh wrote in one real run, kept as they were: the whole demo is printed from them with no server and no scan. */
class RecordedScansTest {

    static Report load(String name) throws Exception {
        return Report.parse(Files.readString(Path.of("src", "test", "resources", "scans", name + ".json")));
    }

    static TlsAuditDemo.Scans scans() throws Exception {
        return new TlsAuditDemo.Scans(load("good"), load("copied"), load("legacy"), load("expired"), load("wrong"), load("address"));
    }

    @Test
    void theRecordedScansPrintTheGoldenOutput() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), TlsAuditDemo.print(scans()));
    }

    @Test
    void aGateOnTestsslsSeverityCannotTellTheThreeServersApart() throws Exception {
        TlsAuditDemo.Scans s = scans();
        List<String> same = List.of("cert_revocation");
        assertEquals(same, s.good().idsAt("HIGH", "CRITICAL"));
        assertEquals(same, s.copied().idsAt("HIGH", "CRITICAL"));
        assertEquals(same, s.legacy().idsAt("HIGH", "CRITICAL"));
        assertEquals("pass", Policy.verdict(s.good()));
        assertEquals(List.of("protocols", "tls13", "forward-secrecy", "obsolete-suites", "hsts", "headers"), Policy.failures(s.legacy()));
    }

    @Test
    void theHstsTimeWorkedOutGivesTestsslsVerdict() throws Exception {
        Report good = load("good");
        WorkedOut.Hsts hsts = WorkedOut.hsts(good.text("HSTS_time"));
        assertEquals(730, hsts.days());
        assertEquals(180, hsts.minimumDays());
        assertEquals(hsts.ok(), good.severity("HSTS_time").equals("OK"));
    }

    @Test
    void theNameCheckByHandAgreesWithTestssl() throws Exception {
        for (String name : List.of("good", "wrong")) {
            Report r = load(name);
            assertEquals(WorkedOut.listed(TlsAuditDemo.HOST, r.text("cert_subjectAltName")), r.severity("cert_trust").equals("OK"), name);
        }
        Report byAddress = load("address");
        assertFalse(WorkedOut.listed("127.0.0.1", byAddress.text("cert_subjectAltName")));
        assertEquals("HIGH", byAddress.severity("cert_trust"));
    }

    @Test
    void aWildcardCoversExactlyOneLabel() {
        assertTrue(WorkedOut.listed("www.shop.localhost", "*.shop.localhost"));
        assertFalse(WorkedOut.listed("a.www.shop.localhost", "*.shop.localhost"));
        assertFalse(WorkedOut.listed("shop.localhost", "*.shop.localhost"));
    }

    @Test
    void theLifetimesAreTheOnesTheCertificatesWereIssuedWith() throws Exception {
        Report good = load("good");
        Report expired = load("expired");
        assertEquals(2160 + 1, WorkedOut.lifetimeHours(good.text("cert_notBefore"), good.text("cert_notAfter")));
        assertEquals(72 - 24, WorkedOut.lifetimeHours(expired.text("cert_notBefore"), expired.text("cert_notAfter")));
    }

    @Test
    void aVersionInTheBannerIsNeverPrinted() throws Exception {
        assertEquals("nginx/<version>", TlsAuditDemo.finding(load("copied"), "banner_server"));
    }
}
