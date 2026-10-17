package com.jk.explore.security.httpnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** These tests need no Docker: they check the configuration text, the auditor and the reply parser. */
class ConfigAndAuditTest {

    private static String conf(NginxConfig.Variant variant) {
        return NginxConfig.text(variant);
    }

    private static Map<String, String> allHeaders() {
        Map<String, String> headers = new HashMap<>();
        NginxConfig.HEADERS.forEach(h -> headers.put(h[0], h[1]));
        return headers;
    }

    @Test
    void theImageIsPinnedToAnExactVersion() {
        assertEquals("nginx:1.31.7-alpine", Nginx.IMAGE);
    }

    @Test
    void theBareConfigurationAddsNoHeaders() {
        assertFalse(conf(NginxConfig.Variant.BARE).contains("add_header"));
        assertFalse(conf(NginxConfig.Variant.BARE).contains("server_tokens off"));
    }

    @Test
    void theHeadersTheConfigurationAddsScoreFiveOfFive() {
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(allHeaders())));
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of())));
    }

    @Test
    void withoutAlwaysNoHeaderHasTheKeyword() {
        String text = conf(NginxConfig.Variant.HEADERS_WITHOUT_ALWAYS);
        assertEquals(5, text.lines().filter(l -> l.contains("add_header")).count());
        assertFalse(text.contains("always"));
    }

    @Test
    void withAlwaysEveryHeaderHasTheKeyword() {
        String text = conf(NginxConfig.Variant.HEADERS_WITH_ALWAYS);
        assertEquals(5, text.lines().filter(l -> l.contains("add_header") && l.contains("always")).count());
        assertTrue(text.contains("server_tokens off"));
    }

    @Test
    void theTrapNestsAnAddHeaderAndTheFixRepeatsTheFive() {
        long trap = conf(NginxConfig.Variant.INHERITANCE_TRAP).lines().filter(l -> l.contains("add_header")).count();
        long fixed = conf(NginxConfig.Variant.INHERITANCE_FIXED).lines().filter(l -> l.contains("add_header")).count();
        assertEquals(5 + 1, trap);
        assertEquals(5 + 1 + 5, fixed);
        assertTrue(conf(NginxConfig.Variant.INHERITANCE_TRAP).contains("location = /account"));
    }

    @Test
    void theLimitsVariantSetsTheLimits() {
        String text = conf(NginxConfig.Variant.LIMITS);
        assertTrue(text.contains("client_max_body_size 1k;"));
        assertTrue(text.contains("large_client_header_buffers 2 1k;"));
    }

    @Test
    void onlyTheHiddenUpstreamVariantHidesHeaders() {
        assertFalse(conf(NginxConfig.Variant.UPSTREAM_LEAKY).contains("proxy_hide_header"));
        assertTrue(conf(NginxConfig.Variant.UPSTREAM_HIDDEN).contains("proxy_hide_header X-Powered-By;"));
        assertTrue(conf(NginxConfig.Variant.UPSTREAM_HIDDEN).contains("proxy_hide_header X-AspNet-Version;"));
    }

    @Test
    void everyConfigurationHasAHealthCheck() {
        for (NginxConfig.Variant variant : NginxConfig.Variant.values()) {
            assertTrue(conf(variant).contains("location = /health"), variant.name());
        }
    }

    @Test
    void aWeakHeaderScoresLessThanTheRightOne() {
        Map<String, String> headers = allHeaders();
        headers.put("Content-Security-Policy", "default-src 'self' 'unsafe-inline'");
        headers.put("Strict-Transport-Security", "max-age=60");
        assertEquals(3, HeaderAudit.score(HeaderAudit.audit(headers)));
    }

    @Test
    void parserReadsStatusHeadersAndBody() throws IOException {
        byte[] raw = "HTTP/1.1 404 Not Found\r\nServer: nginx\r\nX-A: 1\r\nX-A: 2\r\n\r\nnot found".getBytes(StandardCharsets.ISO_8859_1);
        Probe.Reply reply = Probe.parse(raw);
        assertEquals(404, reply.status());
        assertEquals("nginx", reply.headers().get("server"));
        assertEquals("1, 2", reply.headers().get("x-a"));
        assertEquals("not found", reply.body());
    }

    @Test
    void parserRefusesAnIncompleteOrForeignReply() {
        assertThrows(IOException.class, () -> Probe.parse("HTTP/1.1 200 OK\r\nServer: x".getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(IOException.class, () -> Probe.parse("garbage\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)));
    }
}
