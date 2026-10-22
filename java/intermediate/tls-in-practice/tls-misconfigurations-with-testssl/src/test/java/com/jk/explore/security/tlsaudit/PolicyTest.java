package com.jk.explore.security.tlsaudit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The report reader, the policy and the nginx configuration, with no server and no scan. */
class PolicyTest {

    private static final String GOOD = """
            [{"id":"TLS1","severity":"INFO","finding":"not offered"},
             {"id":"TLS1_1","severity":"INFO","finding":"not offered"},
             {"id":"TLS1_2","severity":"OK","finding":"offered"},
             {"id":"TLS1_3","severity":"OK","finding":"offered with final"},
             {"id":"cipherlist_STRONG_FS","severity":"OK","finding":"offered"},
             {"id":"cipherlist_OBSOLETED","severity":"INFO","finding":"not offered"},
             {"id":"cert_chain_of_trust","severity":"OK","finding":"passed."},
             {"id":"cert_trust","severity":"OK","finding":"Ok via SAN and CN"},
             {"id":"cert_expirationStatus","severity":"OK","finding":"90 >= 60 days"},
             {"id":"HSTS_time","severity":"OK","finding":"730 days"},
             {"id":"banner_server","severity":"INFO","finding":"nginx"},
             {"id":"cert_revocation","severity":"HIGH","finding":"Neither CRL nor OCSP URI provided"}]""";

    private static Report with(String id, String severity, String text) {
        String json = GOOD.replaceAll("\\{\"id\":\"" + id + "\",\"severity\":\"[A-Z]+\",\"finding\":\"[^\"]*\"\\}", "{\"id\":\"" + id + "\",\"severity\":\"" + severity + "\",\"finding\":\"" + text + "\"}");
        return Report.parse(json);
    }

    @Test
    void theReportIsReadByIdWithItsSeverity() {
        Report r = Report.parse(GOOD);
        assertTrue(r.offered("TLS1_3"));
        assertEquals("HIGH", r.severity("cert_revocation"));
        assertEquals("MISSING", r.severity("nothing"));
        assertEquals(List.of("cert_revocation"), r.idsAt("HIGH", "CRITICAL"));
    }

    @Test
    void aGoodServerPassesBecauseTheOnlyBrokenRuleHasAnException() {
        Report r = Report.parse(GOOD);
        assertEquals(List.of("revocation"), Policy.broken(r));
        assertEquals("pass", Policy.verdict(r));
    }

    @Test
    void anOldProtocolBreaksTheProtocolRuleEvenThoughTestsslCallsItLow() {
        Report r = with("TLS1", "LOW", "offered (deprecated)");
        assertEquals("LOW", r.severity("TLS1"));
        assertEquals(List.of("protocols"), Policy.failures(r));
    }

    @Test
    void anExpiredCertificateAndAMissingHstsAreEachFoundByTheirOwnRule() {
        assertEquals(List.of("expiry"), Policy.failures(with("cert_expirationStatus", "CRITICAL", "expired")));
        assertEquals(List.of("hsts"), Policy.failures(with("HSTS_time", "LOW", "not offered")));
    }

    @Test
    void aBannerWithAVersionBreaksTheBannerRule() {
        assertEquals(List.of("banner"), Policy.failures(with("banner_server", "INFO", "nginx/1.31.8")));
    }

    @Test
    void everyRuleHasAnIdAndAText() {
        assertEquals(Policy.RULES.size(), Policy.text().size());
        assertTrue(Policy.text().values().stream().noneMatch(String::isBlank));
    }

    @Test
    void theGuidedNginxConfigurationNamesCiphersAndHeadersAndTheCopiedOneDoesNot() throws Exception {
        try (Nginx nginx = new Nginx(java.nio.file.Files.createTempDirectory("nginx-config"))) {
            String guided = nginx.config(Nginx.Settings.guided(), "c.crt", "c.key");
            String copied = nginx.config(Nginx.Settings.copied(), "c.crt", "c.key");
            assertTrue(guided.contains("ssl_ciphers") && guided.contains("Strict-Transport-Security") && guided.contains("server_tokens off"));
            assertTrue(!copied.contains("ssl_ciphers") && !copied.contains("Strict-Transport-Security") && copied.contains("server_tokens on"));
        }
    }
}
