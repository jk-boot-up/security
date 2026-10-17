package com.jk.explore.security.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CookieAndHeaderTest {

    @Test
    void goodCookieHasNoProblems() throws Exception {
        assertTrue(Cookie.parse("session=a; Secure; HttpOnly; SameSite=Lax; Path=/").problems().isEmpty());
    }

    @Test
    void bareCookieHasThreeProblems() throws Exception {
        assertEquals(3, Cookie.parse("session=a").problems().size());
    }

    @Test
    void attributesAreCaseInsensitive() throws Exception {
        Cookie cookie = Cookie.parse("s=a; SECURE; httponly; samesite=Strict");
        assertTrue(cookie.secure() && cookie.httpOnly());
        assertEquals("Strict", cookie.sameSite());
    }

    @Test
    void sameSiteNoneNeedsSecure() throws Exception {
        assertTrue(Cookie.parse("s=a; HttpOnly; SameSite=None").problems().stream().anyMatch(p -> p.contains("SameSite=None")));
        assertTrue(Cookie.parse("s=a; Secure; HttpOnly; SameSite=None").problems().isEmpty());
    }

    @Test
    void hostPrefixRules() throws Exception {
        assertFalse(Cookie.parse("__Host-s=a; Secure; HttpOnly; SameSite=Strict; Path=/").problems().stream().anyMatch(p -> p.contains("__Host-")));
        assertTrue(Cookie.parse("__Host-s=a; Secure; HttpOnly; SameSite=Strict; Path=/; Domain=x.example").problems().stream().anyMatch(p -> p.contains("__Host-")));
    }

    @Test
    void cookieWithoutNameIsRefused() {
        assertThrows(BadMessageException.class, () -> Cookie.parse("=a"));
    }

    @Test
    void emptyResponseScoresZeroAndHardenedScoresFive() {
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of())));
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(LoopbackExchange.HARDENED)));
    }

    @Test
    void headerNamesAreCaseInsensitive() {
        assertEquals(1, HeaderAudit.score(HeaderAudit.audit(Map.of("x-content-type-options", "nosniff"))));
    }

    @Test
    void weakValuesFail() {
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of("Strict-Transport-Security", "max-age=300",
                "Content-Security-Policy", "script-src 'unsafe-inline'", "X-Content-Type-Options", "sniff",
                "X-Frame-Options", "ALLOWALL", "Referrer-Policy", "unsafe-url"))));
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of("Strict-Transport-Security", "includeSubDomains",
                "Content-Security-Policy", "img-src 'self'"))));
    }

    @Test
    void realExchangeCarriesTheHeaders() throws Exception {
        LoopbackExchange.Result plain = LoopbackExchange.fetch(Map.of());
        assertEquals(200, plain.status());
        assertEquals("order A-1001 received", plain.body());
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(plain.headers())));
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(LoopbackExchange.fetch(LoopbackExchange.HARDENED).headers())));
    }
}
