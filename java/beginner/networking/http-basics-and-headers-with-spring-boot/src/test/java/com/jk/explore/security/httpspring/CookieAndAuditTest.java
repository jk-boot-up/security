package com.jk.explore.security.httpspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CookieAndAuditTest {

    @Test
    void goodCookieHasNoProblems() {
        assertTrue(Cookie.parse("session=abc; Secure; HttpOnly; SameSite=Lax; Path=/").problems().isEmpty());
    }

    @Test
    void bareCookieHasThreeProblems() {
        assertEquals(3, Cookie.parse("session=abc").problems().size());
    }

    @Test
    void cookieWithoutANameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Cookie.parse("=abc"));
    }

    @Test
    void hostPrefixNeedsSecurePathAndNoDomain() {
        assertFalse(Cookie.parse("__Host-s=1; Secure; HttpOnly; SameSite=Strict; Path=/; Domain=shop.example").problems().isEmpty());
        assertTrue(Cookie.parse("__Host-s=1; Secure; HttpOnly; SameSite=Strict; Path=/").problems().isEmpty());
    }

    @Test
    void auditScoresEachHeaderOnce() {
        Map<String, String> good = Map.of(
                "Strict-Transport-Security", "max-age=31536000; includeSubDomains",
                "Content-Security-Policy", "default-src 'self'",
                "X-Content-Type-Options", "nosniff",
                "X-Frame-Options", "DENY",
                "Referrer-Policy", "no-referrer");
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(good)));
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(Map.of())));
    }

    @Test
    void weakValuesDoNotScore() {
        Map<String, String> weak = Map.of(
                "Strict-Transport-Security", "max-age=60",
                "Content-Security-Policy", "default-src 'self' 'unsafe-inline'",
                "X-Content-Type-Options", "none",
                "X-Frame-Options", "ALLOW-FROM x",
                "Referrer-Policy", "unsafe-url");
        assertEquals(0, HeaderAudit.score(HeaderAudit.audit(weak)));
    }
}
