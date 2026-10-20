package com.jk.explore.security.tlscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TlsCheckTest {

    private static final Instant NOW = Profiles.NOW;
    private final Checker checker = new Checker();

    private static ServerProfile with(ServerProfile s, Set<String> protocols, List<String> suites) {
        return new ServerProfile(s.host(), protocols, suites, s.serverChoosesCipher(), s.compression(), s.secureRenegotiation(), s.certificate(), s.http());
    }

    private static ServerProfile withCert(ServerProfile s, CertFacts c) {
        return new ServerProfile(s.host(), s.protocols(), s.cipherSuites(), s.serverChoosesCipher(), s.compression(), s.secureRenegotiation(), c, s.http());
    }

    private static ServerProfile withHttp(ServerProfile s, HttpFacts h) {
        return new ServerProfile(s.host(), s.protocols(), s.cipherSuites(), s.serverChoosesCipher(), s.compression(), s.secureRenegotiation(), s.certificate(), h);
    }

    private static CertFacts cert(ServerProfile s, java.util.function.UnaryOperator<CertFacts> change) {
        return change.apply(s.certificate());
    }

    private static CertFacts copy(CertFacts c, List<String> names, Instant from, Instant to, String type, int bits, String sig, boolean self, boolean complete, boolean trusted) {
        return new CertFacts(names, from, to, type, bits, sig, self, complete, trusted);
    }

    @Test
    void aGoodServerGetsAnAWithNoFindings() {
        Report r = checker.check(Profiles.good(), NOW);
        assertEquals('A', r.grade());
        assertEquals(100, r.score());
        assertTrue(r.findings().isEmpty());
    }

    @Test
    void oldProtocolsAndMissingTls13AreFound() {
        ServerProfile g = Profiles.good();
        Report r = checker.check(with(g, Set.of("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"), g.cipherSuites()), NOW);
        assertTrue(r.has("old-protocol-sslv3") && r.has("old-protocol-tlsv1") && r.has("old-protocol-tlsv1_1") && r.has("no-tls13"));
        assertEquals(Severity.CRITICAL, r.findings().stream().filter(f -> f.rule().equals("old-protocol-sslv3")).findFirst().orElseThrow().severity());
        assertEquals('F', r.grade());
        assertTrue(checker.check(with(g, Set.of("TLSv1"), g.cipherSuites()), NOW).has("no-modern-protocol"));
        assertFalse(checker.check(with(g, Set.of("TLSv1.3"), g.cipherSuites()), NOW).has("no-tls13"));
    }

    @Test
    void cipherSuitesAreClassified() {
        assertTrue(CipherSuites.broken("TLS_RSA_EXPORT_WITH_RC4_40_MD5"));
        assertTrue(CipherSuites.broken("TLS_DH_anon_WITH_AES_128_CBC_SHA"));
        assertTrue(CipherSuites.broken("TLS_RSA_WITH_3DES_EDE_CBC_SHA"));
        assertTrue(CipherSuites.broken("TLS_RSA_WITH_NULL_SHA"));
        assertFalse(CipherSuites.broken("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"));
        assertFalse(CipherSuites.lacksForwardSecrecy("TLS_AES_128_GCM_SHA256"));
        assertFalse(CipherSuites.lacksForwardSecrecy("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"));
        assertFalse(CipherSuites.lacksForwardSecrecy("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"));
        assertTrue(CipherSuites.lacksForwardSecrecy("TLS_RSA_WITH_AES_128_GCM_SHA256"));
        assertTrue(CipherSuites.cbc("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"));
    }

    @Test
    void ciphersGiveTheRightFindings() {
        ServerProfile g = Profiles.good();
        Report r = checker.check(with(g, g.protocols(), List.of("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")), NOW);
        assertTrue(r.has("broken-cipher") && r.has("no-forward-secrecy") && r.has("cbc-suites"));
        assertFalse(r.findings().stream().filter(f -> f.rule().equals("no-forward-secrecy")).anyMatch(f -> f.message().contains("RC4")), "a broken suite is reported once");
    }

    @Test
    void certificateDatesNamesChainsAndKeysAreChecked() {
        ServerProfile g = Profiles.good();
        CertFacts c = g.certificate();
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), NOW.minusSeconds(1), "EC", 256, "SHA256withECDSA", false, true, true)), NOW).has("cert-expired"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), NOW.plusSeconds(1), NOW.plus(Duration.ofDays(60)), "EC", 256, "SHA256withECDSA", false, true, true)), NOW).has("cert-not-yet-valid"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), NOW.plus(Duration.ofDays(13)), "EC", 256, "SHA256withECDSA", false, true, true)), NOW).has("cert-expires-soon"));
        assertFalse(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), NOW.plus(Duration.ofDays(15)), "EC", 256, "SHA256withECDSA", false, true, true)), NOW).has("cert-expires-soon"));
        assertTrue(checker.check(withCert(g, copy(c, List.of("other.example"), c.notBefore(), c.notAfter(), "EC", 256, "SHA256withECDSA", false, true, true)), NOW).has("name-mismatch"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), c.notAfter(), "EC", 256, "SHA256withECDSA", true, true, false)), NOW).has("untrusted-chain"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), c.notAfter(), "EC", 256, "SHA256withECDSA", false, false, true)), NOW).has("incomplete-chain"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), c.notAfter(), "RSA", 1024, "SHA256withRSA", false, true, true)), NOW).has("weak-key"));
        assertFalse(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), c.notAfter(), "RSA", 2048, "SHA256withRSA", false, true, true)), NOW).has("weak-key"));
        assertTrue(checker.check(withCert(g, copy(c, c.dnsNames(), c.notBefore(), c.notAfter(), "RSA", 2048, "MD5withRSA", false, true, true)), NOW).has("weak-signature"));
        assertEquals(c, cert(g, x -> x));
    }

    @Test
    void wildcardsCoverOneLabelOnly() {
        assertTrue(Checker.nameMatches("*.shop.example", "www.shop.example"));
        assertFalse(Checker.nameMatches("*.shop.example", "a.b.shop.example"));
        assertFalse(Checker.nameMatches("*.shop.example", "shop.example"));
        assertTrue(Checker.nameMatches("SHOP.example", "shop.EXAMPLE"));
    }

    @Test
    void httpSettingsAreChecked() {
        ServerProfile g = Profiles.good();
        assertTrue(checker.check(withHttp(g, new HttpFacts(-1, false, true, true)), NOW).has("no-hsts"));
        assertTrue(checker.check(withHttp(g, new HttpFacts(3600, false, true, true)), NOW).has("short-hsts"));
        assertTrue(checker.check(withHttp(g, new HttpFacts(63_072_000, false, true, true)), NOW).has("hsts-no-subdomains"));
        assertTrue(checker.check(withHttp(g, new HttpFacts(63_072_000, true, false, true)), NOW).has("no-https-redirect"));
        assertTrue(checker.check(withHttp(g, new HttpFacts(63_072_000, true, true, false)), NOW).has("cookie-not-secure"));
    }

    @Test
    void gradesFollowScoreAndCaps() {
        assertEquals('F', checker.check(Profiles.legacy(), NOW).grade());
        assertEquals('F', checker.check(Profiles.brokenCertificate(), NOW).grade());
        assertEquals('D', checker.check(Profiles.forgotHttp(), NOW).grade());
        ServerProfile g = Profiles.good();
        ServerProfile onlyOneHigh = new ServerProfile(g.host(), g.protocols(), g.cipherSuites(), true, true, true, g.certificate(), g.http());
        Report r = checker.check(onlyOneHigh, NOW);
        assertEquals(80, r.score());
        assertEquals('C', r.grade(), "a high finding caps the grade at C");
    }

    @Test
    void findingsAreSortedWorstFirst() {
        List<Finding> f = checker.check(Profiles.legacy(), NOW).findings();
        for (int i = 1; i < f.size(); i++) {
            assertTrue(f.get(i - 1).severity().compareTo(f.get(i).severity()) >= 0);
        }
    }

    @Test
    void thePoliciesFixProtocolsAndCiphersAndNothingElse() {
        Report after = checker.check(Policy.INTERMEDIATE.applyTo(Profiles.legacy()), NOW);
        assertFalse(after.has("broken-cipher") || after.has("old-protocol-tlsv1") || after.has("tls-compression") || after.has("insecure-renegotiation"));
        assertTrue(after.has("weak-key") && after.has("weak-signature"));
        assertEquals('A', checker.check(Policy.MODERN.applyTo(Profiles.good()), NOW).grade());
        assertTrue(Policy.INTERMEDIATE.nginx().contains("ssl_protocols TLSv1.2 TLSv1.3;"));
        assertTrue(Policy.INTERMEDIATE.nginx().contains("ECDHE-RSA-AES128-GCM-SHA256"));
        assertFalse(Policy.INTERMEDIATE.nginx().contains("TLS_AES"));
    }
}
