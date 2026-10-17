package com.jk.explore.security.xss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
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

    @Test
    void theRealServerSendsAFreshPolicyAndAnHttpOnlyCookie() throws Exception {
        try (ShopServer shop = new ShopServer(List.of(new Review("Ana", "a", "b")))) {
            HttpResponse<String> one = shop.get();
            HttpResponse<String> two = shop.get();
            String nonce = Markup.scriptNonces(one.body()).get(0);
            assertEquals(200, one.statusCode());
            assertTrue(one.headers().firstValue("Content-Security-Policy").orElseThrow().contains("'nonce-" + nonce + "'"));
            assertNotEquals(nonce, Markup.scriptNonces(two.body()).get(0));
            assertTrue(one.headers().firstValue("Set-Cookie").orElseThrow().contains("HttpOnly"));
        }
    }
}
