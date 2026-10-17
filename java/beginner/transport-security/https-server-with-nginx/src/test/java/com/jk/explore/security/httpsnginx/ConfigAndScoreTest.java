package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigAndScoreTest {

    private static int points(String conf) {
        return ConfigScore.points(ConfigScore.score(conf));
    }

    private static ConfigScore.Finding finding(String conf, String check) {
        return ConfigScore.score(conf).stream().filter(f -> f.check().equals(check)).findFirst().orElseThrow();
    }

    @Test
    void theCarefulConfigurationScoresFullMarks() {
        assertEquals(7, points(NginxConfig.text(NginxConfig.Variant.CAREFUL)));
        assertEquals(7, points(NginxConfig.text(NginxConfig.Variant.COMPAT)));
        assertEquals(7, points(NginxConfig.text(NginxConfig.Variant.MTLS)));
    }

    @Test
    void aCopiedConfigurationScoresLow() {
        assertEquals(1, points(HttpsNginxDemo.SLOPPY));
    }

    @Test
    void oldProtocolsAreFoundButTls13IsNotMistakenForTls1() {
        assertFalse(finding("ssl_protocols TLSv1 TLSv1.2;", "modern protocols only").passed());
        assertFalse(finding("ssl_protocols TLSv1.1 TLSv1.2;", "modern protocols only").passed());
        assertTrue(finding("ssl_protocols TLSv1.2 TLSv1.3;", "modern protocols only").passed());
        assertFalse(finding("server { }", "modern protocols only").passed());
    }

    @Test
    void hstsNeedsAlwaysToSurviveErrorPages() {
        assertFalse(finding("add_header Strict-Transport-Security \"max-age=63072000\";", "HSTS on every answer").passed());
        assertTrue(finding("add_header Strict-Transport-Security \"max-age=63072000\" always;", "HSTS on every answer").passed());
    }

    @Test
    void copyingTheClientsForwardedProtoIsFound() {
        String trap = NginxConfig.text(NginxConfig.Variant.PROXY_TRAP);
        assertTrue(trap.contains("proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;"));
        assertFalse(finding(trap, "app told what nginx saw").passed());
        assertFalse(finding("proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;", "app told what nginx saw").passed());
        assertTrue(finding(NginxConfig.text(NginxConfig.Variant.PROXY_SAFE), "app told what nginx saw").passed());
    }

    @Test
    void eachVariantHasItsOwnDirectives() {
        assertTrue(NginxConfig.text(NginxConfig.Variant.MTLS).contains("ssl_verify_client on;"));
        assertFalse(NginxConfig.text(NginxConfig.Variant.CAREFUL).contains("ssl_verify_client"));
        assertTrue(NginxConfig.text(NginxConfig.Variant.IMPOSTOR).contains("/pki/impostor.crt"));
        assertTrue(NginxConfig.text(NginxConfig.Variant.CAREFUL).contains("ssl_protocols TLSv1.3;"));
        assertTrue(NginxConfig.text(NginxConfig.Variant.COMPAT).contains("ssl_protocols TLSv1.2 TLSv1.3;"));
        assertTrue(NginxConfig.text(NginxConfig.Variant.CAREFUL, "missing").contains("/pki/missing.crt"));
    }

    @Test
    void theAuditorCountsFiveHeaders() {
        Map<String, String> good = Map.of("Strict-Transport-Security", "max-age=63072000; includeSubDomains", "Content-Security-Policy", "default-src 'self'",
                "X-Content-Type-Options", "nosniff", "X-Frame-Options", "DENY", "Referrer-Policy", "no-referrer");
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(good)));
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of())));
        assertEquals(List.of("Strict-Transport-Security", "Content-Security-Policy", "X-Content-Type-Options", "X-Frame-Options", "Referrer-Policy"),
                HeaderAudit.audit(good).stream().map(HeaderAudit.Finding::header).toList());
    }
}
