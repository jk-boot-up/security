package com.jk.explore.security.xssspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentSecurityPolicyTest {

    @Test
    void onlyAScriptWithTheResponsesNonceMayRun() {
        String policy = ContentSecurityPolicy.strict("abc");
        assertTrue(ContentSecurityPolicy.allowsInlineScript(policy, "abc"));
        assertFalse(ContentSecurityPolicy.allowsInlineScript(policy, null));
        assertFalse(ContentSecurityPolicy.allowsInlineScript(policy, "abd"));
    }

    @Test
    void unsafeInlineIsIgnoredWhenANonceIsListed() {
        assertTrue(ContentSecurityPolicy.allowsInlineScript("script-src 'unsafe-inline'", null));
        assertFalse(ContentSecurityPolicy.allowsInlineScript("script-src 'nonce-abc' 'unsafe-inline'", null));
    }

    @Test
    void scriptSrcWinsAndDefaultSrcIsTheFallback() {
        assertEquals(List.of("'self'"), ContentSecurityPolicy.scriptSources("default-src 'none'; script-src 'self'"));
        assertEquals(List.of("'self'"), ContentSecurityPolicy.scriptSources("default-src 'self'; img-src *"));
        assertTrue(ContentSecurityPolicy.allowsInlineScript("img-src 'self'", null));
    }

    @Test
    void noncesAre128RandomBitsAndNeverRepeat() {
        String a = ContentSecurityPolicy.newNonce();
        assertEquals(16, Base64.getDecoder().decode(a).length);
        assertNotEquals(a, ContentSecurityPolicy.newNonce());
    }
}
