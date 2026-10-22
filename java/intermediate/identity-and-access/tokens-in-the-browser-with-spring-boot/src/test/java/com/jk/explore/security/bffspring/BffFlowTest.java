package com.jk.explore.security.bffspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jk.explore.security.bffspring.api.Ledger;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

/** A real Keycloak in Docker, the real accounts API and the real backend for the frontend. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BffFlowTest {

    private KeycloakLab keycloak;
    private Running api;
    private Running bff;
    private SpaClient spa;

    @BeforeAll
    void start() throws Exception {
        int port = Running.freePort();
        keycloak = KeycloakLab.start(port);
        api = Running.api(keycloak);
        bff = Running.bff(keycloak, port, api.url());
        spa = new SpaClient(keycloak.issuer(), api.url());
    }

    @AfterAll
    void stop() {
        bff.close();
        api.close();
        keycloak.close();
    }

    private Shopper signedInAtBff() throws Exception {
        Shopper ana = new Shopper();
        ana.navigate(bff.url() + "/oauth2/authorization/keycloak", "ana", "demo-password");
        return ana;
    }

    @Test
    void tokensInTheBrowserWorkForAnyoneWhoCopiesThem() throws Exception {
        SpaClient.Tokens t = spa.signIn("bank-spa", null);
        assertEquals("Bearer", t.string("token_type"));
        assertEquals(200, spa.bearer("GET", "/accounts", t.string("access_token")).status());
        assertEquals(200, spa.refresh("bank-spa", t.string("refresh_token"), null).status());
    }

    @Test
    void aStolenAccessTokenDiesWhenItExpires() throws Exception {
        String token = spa.signIn("bank-spa", null).string("access_token");
        OffsetClock clock = api.bean(OffsetClock.class);
        clock.advance(Duration.ofSeconds(331));
        try {
            assertEquals(401, spa.bearer("GET", "/accounts", token).status());
        } finally {
            clock.advance(Duration.ofSeconds(-331));
        }
    }

    @Test
    void theBackendKeepsTheTokensAndTheBrowserGetsOnlyCookies() throws Exception {
        Shopper ana = signedInAtBff();
        String header = ana.setCookieHeader(bff.url(), "__Host-bff");
        assertTrue(header.contains("Path=/") && header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=Strict") && !header.contains("Domain="));
        assertEquals(Map.of("XSRF-TOKEN", ana.documentCookie(bff.url()).get("XSRF-TOKEN")), ana.documentCookie(bff.url()));
        OAuth2AuthorizedClient held = bff.bean(OAuth2AuthorizedClientService.class).loadAuthorizedClient("keycloak", BffSpringDemo.SUBJECT);
        assertTrue(held.getAccessToken() != null && held.getRefreshToken() != null);
        assertFalse(ana.get(bff.url() + "/").body().contains(held.getAccessToken().getTokenValue()));
        assertEquals(200, ana.get(bff.url() + "/api/accounts").status());
    }

    @Test
    void aRequestWithoutASessionGetsNothing() throws Exception {
        assertEquals(401, new Shopper().get(bff.url() + "/api/accounts").status());
    }

    @Test
    void theBackendRefreshesAnExpiredAccessTokenOnTheServer() throws Exception {
        Shopper ana = signedInAtBff();
        OAuth2AuthorizedClientService service = bff.bean(OAuth2AuthorizedClientService.class);
        String before = service.<OAuth2AuthorizedClient>loadAuthorizedClient("keycloak", BffSpringDemo.SUBJECT).getAccessToken().getTokenValue();
        OffsetClock clock = bff.bean(OffsetClock.class);
        clock.advance(Duration.ofMinutes(6));
        try {
            assertEquals(200, ana.get(bff.url() + "/api/accounts").status());
        } finally {
            clock.advance(Duration.ofMinutes(-6));
        }
        assertNotEquals(before, service.<OAuth2AuthorizedClient>loadAuthorizedClient("keycloak", BffSpringDemo.SUBJECT).getAccessToken().getTokenValue());
    }

    @Test
    void aTransferNeedsTheBanksOwnRequestHeader() throws Exception {
        Shopper ana = signedInAtBff();
        int before = api.bean(Ledger.class).transfers().size();
        assertEquals(403, ana.request("POST", bff.url() + "/api/transfers", Map.of(), "", false).status());
        assertEquals(403, ana.request("POST", bff.url() + "/api/transfers", Map.of("X-XSRF-TOKEN", "guessed"), "", false).status());
        assertEquals(201, ana.request("POST", bff.url() + "/api/transfers", Map.of("X-XSRF-TOKEN", ana.documentCookie(bff.url()).get("XSRF-TOKEN")), "", false).status());
        assertEquals(before + 1, api.bean(Ledger.class).transfers().size());
    }

    @Test
    void aRequestStartedByAnotherSiteCarriesNoSession() throws Exception {
        Shopper ana = signedInAtBff();
        assertEquals(401, ana.request("GET", bff.url() + "/api/accounts", Map.of("Origin", "https://evil.example"), null, true).status());
        assertEquals(200, ana.get(bff.url() + "/api/accounts").status(), "the real session was not replaced by the other site's response");
    }

    @Test
    void theBackendAllowsNoOtherOrigin() throws Exception {
        Shopper ana = signedInAtBff();
        Shopper.Reply pre = ana.request("OPTIONS", bff.url() + "/api/transfers", Map.of("Origin", "https://evil.example", "Access-Control-Request-Method", "POST", "Access-Control-Request-Headers", "x-xsrf-token"), null, true);
        assertNull(pre.headers().get("access-control-allow-origin"));
    }

    @Test
    void aDpopBoundTokenNeedsItsKeysProof() throws Exception {
        DpopKey app = new DpopKey();
        SpaClient.Tokens t = spa.signIn("bank-spa-dpop", app);
        String at = t.string("access_token");
        assertEquals("DPoP", t.string("token_type"));
        assertTrue(String.valueOf(SpaClient.claims(at).get("cnf")).contains(app.thumbprint()));
        assertEquals(200, spa.dpop("GET", "/accounts", at, app.proof("GET", spa.urlOf("/accounts"), at)).status());
        assertEquals("invalid_token", BffSpringDemo.error(spa.bearer("GET", "/accounts", at).challenge()));
        assertEquals(401, spa.dpop("GET", "/accounts", at, null).status());
    }

    @Test
    void aProofFromAnotherKeyOrForAnotherRequestOrAnOldOneOrAReusedOneIsRefused() throws Exception {
        DpopKey app = new DpopKey();
        String at = spa.signIn("bank-spa-dpop", app).string("access_token");
        assertEquals(401, spa.dpop("GET", "/accounts", at, new DpopKey().proof("GET", spa.urlOf("/accounts"), at)).status());
        assertEquals(401, spa.dpop("POST", "/transfers", at, app.proof("GET", spa.urlOf("/accounts"), at)).status());
        assertEquals(401, spa.dpop("GET", "/accounts", at, app.proof("GET", spa.urlOf("/accounts"), at, Instant.now().minusSeconds(120), UUID.randomUUID().toString())).status());
        assertEquals(401, spa.dpop("GET", "/accounts", at, app.proof("GET", spa.urlOf("/accounts"), null)).status());
        String proof = app.proof("GET", spa.urlOf("/accounts"), at);
        assertEquals(200, spa.dpop("GET", "/accounts", at, proof).status());
        assertEquals(401, spa.dpop("GET", "/accounts", at, proof).status());
    }

    @Test
    void aStolenDpopRefreshTokenNeedsTheKeyToo() throws Exception {
        DpopKey app = new DpopKey();
        SpaClient.Tokens t = spa.signIn("bank-spa-dpop", app);
        SpaClient.Tokens stolen = spa.refresh("bank-spa-dpop", t.string("refresh_token"), new DpopKey());
        assertEquals(400, stolen.status());
        assertEquals("DPoP confirmation doesn't match DPoP proof", stolen.string("error_description"));
        assertEquals(400, spa.refresh("bank-spa-dpop", t.string("refresh_token"), null).status());
        assertEquals(200, spa.refresh("bank-spa-dpop", t.string("refresh_token"), app).status());
    }

    @Test
    void aSecondBackendDoesNotKnowTheFirstOnesSessions() throws Exception {
        Shopper ana = signedInAtBff();
        try (Running second = Running.bff(keycloak, Running.freePort(), api.url())) {
            Shopper.Reply r = new Shopper().request("GET", second.url() + "/api/accounts", Map.of("Cookie", "__Host-bff=" + ana.cookieValue(bff.url(), "__Host-bff")), null, false);
            assertEquals(401, r.status());
        }
    }
}
