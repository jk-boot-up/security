package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The Content-Security-Policy check passes only a policy that really limits where scripts come from. */
class CspCheckTest {

    private static HeaderAudit.Finding csp(String policy) {
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(Map.of("Content-Security-Policy", policy));
        return findings.stream().filter(f -> f.header().equals("Content-Security-Policy")).findFirst().orElseThrow();
    }

    @Test
    void aPolicyThatAllowsAnySiteFails() {
        assertEquals("lets scripts load from anywhere", csp("default-src *").reason());
        assertEquals("lets scripts load from anywhere", csp("default-src 'self'; script-src https:").reason());
        assertEquals("lets scripts load from anywhere", csp("script-src data:").reason());
    }

    @Test
    void aRestrictivePolicyPasses() {
        assertEquals(true, csp("default-src 'self'").passed());
        assertEquals(true, csp("default-src 'none'; script-src 'self' https://cdn.shop.example").passed());
    }

    @Test
    void scriptSrcWinsOverDefaultSrc() {
        assertEquals(true, csp("default-src *; script-src 'self'").passed());
    }

    @Test
    void aPolicyWithoutScriptRulesOrWithUnsafeKeywordsFails() {
        assertEquals("no default-src or script-src", csp("img-src 'self'").reason());
        assertEquals("allows unsafe-inline or unsafe-eval", csp("default-src 'self' 'unsafe-inline'").reason());
    }
}
