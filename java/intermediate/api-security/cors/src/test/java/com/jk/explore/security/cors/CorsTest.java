package com.jk.explore.security.cors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CorsTest {

    private static FetchResult read(CorsPolicy policy, String origin) {
        return new Browser(new BankApi(policy, true)).fetch(origin, "GET", "/account", null, true);
    }

    @Test
    void withoutCorsHeadersAPageCanSendButNotRead() {
        BankApi bank = new BankApi(CorsPolicy.NONE, false);
        FetchResult r = new Browser(bank).fetch(CorsDemo.EVIL, "POST", "/transfer", "text/plain", true);
        assertTrue(r.reachedBank());
        assertFalse(r.readable());
        assertEquals(List.of("POST /transfer"), bank.processed());
    }

    @Test
    void nonSimpleRequestsAreStoppedByTheFailedPreflight() {
        BankApi bank = new BankApi(CorsPolicy.NONE, false);
        FetchResult r = new Browser(bank).fetch(CorsDemo.EVIL, "PUT", "/transfer", "application/json", true);
        assertFalse(r.reachedBank());
        assertTrue(bank.processed().isEmpty());
    }

    @Test
    void reflectingTheOriginLetsAnySiteReadWithCookies() {
        assertTrue(read(CorsPolicy.REFLECT_ANY, CorsDemo.EVIL).readable());
        assertTrue(read(CorsPolicy.REFLECT_ANY, CorsDemo.OPAQUE).readable());
    }

    @Test
    void allowingNullLetsSandboxedPagesRead() {
        CorsPolicy p = CorsPolicy.exactAndNull(CorsDemo.PARTNERS);
        assertTrue(read(p, CorsDemo.OPAQUE).readable());
        assertFalse(read(p, CorsDemo.EVIL).readable());
    }

    @Test
    void suffixMatchingLetsALookAlikeRead() {
        CorsPolicy p = CorsPolicy.suffix("bank.example");
        assertTrue(read(p, CorsDemo.LOOKALIKE).readable());
        assertFalse(read(p, CorsDemo.EVIL).readable());
    }

    @Test
    void exactListAllowsOnlyListedOriginsCaseSensitively() {
        CorsPolicy p = CorsPolicy.exact(CorsDemo.PARTNERS);
        assertTrue(read(p, CorsDemo.PARTNER).readable());
        for (String bad : new String[] {CorsDemo.EVIL, CorsDemo.LOOKALIKE, CorsDemo.OPAQUE, "https://PARTNER.example", "https://partner.example.evil.example",
                "http://partner.example", "https://partner.example:8443"}) {
            assertFalse(read(p, bad).readable(), bad);
        }
        assertEquals(java.util.Optional.empty(), p.allow(null));
    }

    @Test
    void preflightLetsAListedSiteSendJsonPut() {
        BankApi bank = new BankApi(CorsPolicy.exact(CorsDemo.PARTNERS), true);
        FetchResult r = new Browser(bank).fetch(CorsDemo.PARTNER, "PUT", "/transfer", "application/json", true);
        assertTrue(r.reachedBank() && r.readable());
    }

    @Test
    void preflightWithoutAllowedMethodsOrHeadersBlocksTheRealRequest() {
        BankApi bank = new BankApi(CorsPolicy.exact(CorsDemo.PARTNERS), true);
        assertFalse(new Browser(bank).fetch(CorsDemo.PARTNER, "DELETE", "/transfer", null, true).reachedBank());
    }

    @Test
    void responsesVaryByOriginAndPublicDataUsesTheWildcard() {
        BankApi bank = new BankApi(CorsPolicy.exact(CorsDemo.PARTNERS), true);
        Response r = bank.handle(new Request("GET", "/account", Map.of("origin", CorsDemo.PARTNER), true));
        assertEquals("Origin", r.header("vary"));
        assertEquals(CorsDemo.PARTNER, r.header("access-control-allow-origin"));
        assertEquals("true", r.header("access-control-allow-credentials"));
        Response none = bank.handle(new Request("GET", "/account", Map.of("origin", CorsDemo.EVIL), true));
        assertNull(none.header("access-control-allow-origin"));
        assertEquals("Origin", none.header("vary"));
        assertEquals("*", bank.handle(new Request("GET", "/rates", Map.of("origin", CorsDemo.EVIL), false)).header("access-control-allow-origin"));
    }

    @Test
    void theBrowserNeverAcceptsAWildcardWithCookies() {
        Response star = Response.of(200, "x").with("access-control-allow-origin", "*").with("access-control-allow-credentials", "true");
        assertFalse(Browser.approves(star, CorsDemo.EVIL, true));
        assertTrue(Browser.approves(star, CorsDemo.EVIL, false));
        assertFalse(Browser.approves(Response.of(200, "x"), CorsDemo.EVIL, false));
    }

    @Test
    void sameOriginNeedsNoCors() {
        assertTrue(new Browser(new BankApi(CorsPolicy.NONE, false)).fetch(BankApi.ORIGIN, "GET", "/account", null, true).readable());
    }
}
