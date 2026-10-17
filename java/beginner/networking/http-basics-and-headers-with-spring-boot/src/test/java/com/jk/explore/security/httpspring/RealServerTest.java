package com.jk.explore.security.httpspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real servers on the loopback interface, started on free ports and always closed. */
class RealServerTest {

    @Test
    void headersSwitchedOffScoreZero() throws Exception {
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare()) {
            assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Wire.get(bare.port(), "/products", Map.of()).firstValues())));
        }
    }

    @Test
    void hardenedStoreScoresFiveBehindAProxyThatEndsHttps() throws Exception {
        try (HttpSpringDemo.Running hardened = HttpSpringDemo.hardened()) {
            Wire.Reply plain = Wire.get(hardened.port(), "/products", Map.of());
            Wire.Reply proxied = Wire.get(hardened.port(), "/products", Map.of("X-Forwarded-Proto", "https"));
            assertEquals(4, HeaderAudit.score(HeaderAudit.audit(plain.firstValues())));
            assertEquals(5, HeaderAudit.score(HeaderAudit.audit(proxied.firstValues())));
        }
    }

    @Test
    void sessionCookieIsHardenedByThreeProperties() throws Exception {
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare(); HttpSpringDemo.Running hardened = HttpSpringDemo.hardened()) {
            Cookie plain = Cookie.parse(Wire.get(bare.port(), "/cart", Map.of()).all("Set-Cookie").get(0));
            Cookie strict = Cookie.parse(Wire.get(hardened.port(), "/cart", Map.of()).all("Set-Cookie").get(0));
            assertTrue(plain.httpOnly());
            assertEquals(2, plain.problems().size());
            assertTrue(strict.secure() && strict.httpOnly());
            assertEquals("Strict", strict.sameSite());
            assertTrue(strict.problems().isEmpty());
        }
    }

    @Test
    void lineBreakInAHeaderIsRefusedByTheContainer() throws Exception {
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare()) {
            Wire.Reply reply = Wire.get(bare.port(), "/echo?name=a%0d%0aSet-Cookie:%20evil=1", Map.of());
            assertEquals(500, reply.status());
            assertTrue(reply.all("Set-Cookie").isEmpty());
            assertTrue(reply.all("X-Echo").isEmpty());
        }
    }

    @Test
    void malformedHeaderNameIsRefused() throws Exception {
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare()) {
            assertEquals(400, Wire.rawStatus(bare.port(), "GET /products HTTP/1.1\r\nHost: x\r\nBad Header: x\r\n\r\n"));
        }
    }

    @Test
    void headerSizeLimitSeparatesTheTwoStores() throws Exception {
        String big = "GET /products HTTP/1.1\r\nHost: x\r\nX-Padding: " + "a".repeat(4000) + "\r\nConnection: close\r\n\r\n";
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare(); HttpSpringDemo.Running hardened = HttpSpringDemo.hardened()) {
            assertEquals(200, Wire.rawStatus(bare.port(), big));
            assertEquals(400, Wire.rawStatus(hardened.port(), big));
        }
    }

    @Test
    void forwardedHeaderIsIgnoredWhenTheStoreIsNotConfiguredToTrustIt() throws Exception {
        try (HttpSpringDemo.Running bare = HttpSpringDemo.bare()) {
            Wire.Reply reply = Wire.get(bare.port(), "/products", Map.of("X-Forwarded-Proto", "https"));
            assertFalse(reply.all("Strict-Transport-Security").size() > 0);
        }
    }
}
