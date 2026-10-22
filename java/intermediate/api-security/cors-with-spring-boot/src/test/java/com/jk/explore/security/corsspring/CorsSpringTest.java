package com.jk.explore.security.corsspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Each test starts a real bank on a free loopback port and talks to it the way a browser would. */
class CorsSpringTest {

    private static final String PARTNER = CorsSpringDemo.PARTNER;
    private static final String EVIL = CorsSpringDemo.EVIL;
    private static final String LOOKALIKE = CorsSpringDemo.LOOKALIKE;

    @Test
    void withoutAPolicyAPageCanSendButNotRead() throws Exception {
        try (Running bank = Running.start("none", "security")) {
            Browser b = bank.browser();
            FetchResult get = b.fetch(EVIL, "GET", "/account", null, true);
            assertTrue(get.reachedBank());
            assertFalse(get.readable());
            assertNull(get.body());
            FetchResult post = b.fetch(EVIL, "POST", "/transfer", "text/plain", true);
            assertTrue(post.reachedBank());
            assertFalse(post.readable());
            assertEquals(List.of("GET /account", "POST /transfer"), bank.processed().list());
        }
    }

    @Test
    void aPreflightForJsonStopsTheRequestWhenThereIsNoPolicy() throws Exception {
        try (Running bank = Running.start("none", "security")) {
            FetchResult put = bank.browser().fetch(EVIL, "PUT", "/transfer", "application/json", true);
            assertFalse(put.reachedBank());
            assertEquals(List.of(), bank.processed().list());
        }
    }

    @Test
    void theBanksOwnPageNeedsNoPolicy() throws Exception {
        try (Running bank = Running.start("none", "security")) {
            FetchResult r = bank.browser().fetch(bank.url(), "GET", "/account", null, true);
            assertTrue(r.readable());
            assertEquals("balance: 125000 cents", r.body());
        }
    }

    @Test
    void starWithCredentialsFailsAtTheFirstCrossOriginRequest() throws Exception {
        try (Running bank = Running.start("star-with-credentials", "mvc")) {
            Browser.Reply r = bank.browser().raw("GET", "/account", EVIL, true);
            assertEquals(500, r.status());
            assertNull(r.header("access-control-allow-origin"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {EVIL, LOOKALIKE, "null"})
    void reflectingPatternLetsEveryOriginRead(String origin) throws Exception {
        try (Running bank = Running.start("reflect", "security")) {
            assertTrue(bank.browser().fetch(origin, "GET", "/account", null, true).readable());
        }
    }

    @Test
    void exactListThatAlsoAllowsNullLetsNullRead() throws Exception {
        try (Running bank = Running.start("exact-and-null", "security")) {
            Browser b = bank.browser();
            assertTrue(b.fetch("null", "GET", "/account", null, true).readable());
            assertFalse(b.fetch(EVIL, "GET", "/account", null, true).readable());
            assertFalse(b.fetch(LOOKALIKE, "GET", "/account", null, true).readable());
        }
    }

    @Test
    void patternWithoutADotLetsTheLookalikeRead() throws Exception {
        try (Running bank = Running.start("suffix", "security")) {
            Browser b = bank.browser();
            assertTrue(b.fetch(LOOKALIKE, "GET", "/account", null, true).readable());
            assertFalse(b.fetch(EVIL, "GET", "/account", null, true).readable());
            assertFalse(b.fetch("null", "GET", "/account", null, true).readable());
        }
    }

    @Test
    void exactListAllowsOnlyThePartner() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            assertTrue(b.fetch(PARTNER, "GET", "/account", null, true).readable());
            assertTrue(b.fetch(PARTNER, "PUT", "/transfer", "application/json", true).readable());
            for (String origin : List.of(EVIL, LOOKALIKE, "null")) {
                FetchResult r = b.fetch(origin, "GET", "/account", null, true);
                assertFalse(r.readable(), origin);
                assertFalse(r.reachedBank(), origin);
            }
            assertEquals(List.of("GET /account", "PUT /transfer"), bank.processed().list());
        }
    }

    @Test
    void unlistedOriginGets403BeforeTheController() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser.Reply r = bank.browser().raw("GET", "/account", EVIL, true);
            assertEquals(403, r.status());
            assertEquals("Invalid CORS request", r.body());
            assertEquals(List.of(), bank.processed().list());
        }
    }

    @Test
    void varyOriginIsSentSoCachesKeepOneCopyPerOrigin() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            assertTrue(bank.browser().raw("GET", "/account", PARTNER, true).header("vary").contains("Origin"));
        }
    }

    @Test
    void publicRatesAreReadableByAnyPageWithoutCredentials() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            assertTrue(b.fetch(EVIL, "GET", "/rates", null, false).readable());
            assertFalse(b.fetch(EVIL, "GET", "/rates", null, true).readable(), "a wildcard is refused when credentials are involved");
        }
    }

    @Test
    void thePolicyMustBeRegisteredBeforeTheLoginCheck() throws Exception {
        try (Running bank = Running.start("exact", "mvc")) {
            Browser b = bank.browser();
            assertEquals(401, b.preflight(PARTNER, "PUT", "/transfer", true).status());
            assertFalse(b.fetch(PARTNER, "PUT", "/transfer", "application/json", true).reachedBank());
            assertTrue(b.fetch(PARTNER, "GET", "/account", null, true).readable(), "a simple request has no preflight");
        }
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            assertEquals(200, b.preflight(PARTNER, "PUT", "/transfer", true).status());
            assertTrue(b.fetch(PARTNER, "PUT", "/transfer", "application/json", true).readable());
        }
    }

    @Test
    void thePreflightNeverCarriesCredentials() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser.Reply r = bank.browser().preflight(PARTNER, "PUT", "/transfer", true);
            assertEquals(200, r.status());
            assertEquals(PARTNER, r.header("access-control-allow-origin"));
            assertEquals("600", r.header("access-control-max-age"));
        }
    }

    @Test
    void corsIsNotADefenceAgainstACallerThatSendsNoOrigin() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser b = bank.browser();
            assertFalse(b.fetch(EVIL, "POST", "/transfer", "text/plain", true).reachedBank());
            Browser.Reply r = b.raw("POST", "/transfer", null, true);
            assertEquals(200, r.status());
            assertEquals(List.of("POST /transfer"), bank.processed().list());
        }
    }

    @Test
    void withoutCredentialsTheBankRefusesEvenAListedOrigin() throws Exception {
        try (Running bank = Running.start("exact", "security")) {
            Browser.Reply r = bank.browser().raw("GET", "/account", PARTNER, false);
            assertEquals(401, r.status());
            assertEquals(List.of(), bank.processed().list());
        }
    }
}
