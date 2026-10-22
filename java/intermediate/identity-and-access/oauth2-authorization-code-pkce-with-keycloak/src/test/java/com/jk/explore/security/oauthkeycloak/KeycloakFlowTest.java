package com.jk.explore.security.oauthkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real Keycloak in Docker and a real resource server. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeycloakFlowTest {

    private KeycloakLab keycloak;
    private Running api;
    private OAuthClient app;

    @BeforeAll
    void start() throws Exception {
        keycloak = KeycloakLab.start();
        api = Running.start(keycloak);
        app = new OAuthClient(keycloak.base(), keycloak.issuer());
    }

    @AfterAll
    void stop() {
        api.close();
        keycloak.close();
    }

    private OAuthClient.TokenReply tokens(String user, String scope) throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, user, scope);
        return app.token(OAuthKeycloakDemo.exchange(s.code(), s.verifier()));
    }

    @Test
    void theFlowGivesAnAccessTokenForTheAccountsApi() throws Exception {
        OAuthClient.TokenReply t = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        assertTrue(t.ok());
        assertEquals("300", t.string("expires_in"));
        Map<String, Object> claims = OAuthKeycloakDemo.claims(t.string("access_token"));
        assertEquals("accounts-api", claims.get("aud"));
        assertEquals("budget-app", claims.get("azp"));
        assertEquals(keycloak.issuer(), claims.get("iss"));
        assertEquals(AccountsController.ANA, claims.get("sub"));
        assertEquals(200, api.get("/accounts", t.string("access_token")).status());
    }

    @Test
    void consentIsAskedTheFirstTimeAndRememberedAfterwards() throws Exception {
        tokens("ben", OAuthKeycloakDemo.ACCOUNTS_READ);
        assertFalse(OAuthKeycloakDemo.start(app, "ben", OAuthKeycloakDemo.ACCOUNTS_READ).outcome().consentShown());
    }

    @Test
    void aWrongPasswordGivesNoCode() throws Exception {
        OAuthClient.Outcome o = app.signIn(OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, OAuthKeycloakDemo.ACCOUNTS_READ, "s", OAuthClient.challenge("v".repeat(43))),
                "ana", "wrong-password");
        assertFalse(o.redirected());
    }

    @Test
    void theApiNeedsAToken() throws Exception {
        assertEquals(401, api.get("/accounts", null).status());
        assertEquals(401, api.get("/accounts", "not-a-token").status());
    }

    @Test
    void aTamperedTokenIsRefusedByTheApi() throws Exception {
        String token = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ).string("access_token");
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new String(java.util.Base64.getUrlDecoder().decode(parts[1])).replace("accounts.read", "payments.write").getBytes()) + "." + parts[2];
        assertEquals(401, api.get("/accounts", forged).status());
    }

    @Test
    void theScopeDecidesWhatTheTokenMayDo() throws Exception {
        String token = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ).string("access_token");
        assertEquals("403 insufficient_scope", api.post("/payments", token).text());
    }

    @Test
    void theIdTokenIsNotAnAccessTokenForTheApi() throws Exception {
        OAuthClient.TokenReply t = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        assertEquals(401, api.get("/accounts", t.string("id_token")).status());
    }

    @Test
    void theApiRefusesATokenAfterItExpires() throws Exception {
        String token = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ).string("access_token");
        assertEquals(200, api.get("/accounts", token).status());
        api.clock().advance(Duration.ofSeconds(331));
        assertEquals(401, api.get("/accounts", token).status());
        api.clock().advance(Duration.ofSeconds(-331));
    }

    @Test
    void aFailedRedemptionBurnsTheCode() throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        String code = s.code();
        assertEquals("invalid_grant: PKCE code verifier not specified", app.token(OAuthKeycloakDemo.exchange(code, null)).error());
        assertEquals("invalid_grant: Code not valid", app.token(OAuthKeycloakDemo.exchange(code, s.verifier())).error());
    }

    @Test
    void aWrongVerifierIsRefusedAndTheRightOneWorksOnAFreshCode() throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        assertEquals("invalid_grant: PKCE verification failed: Code mismatch", app.token(OAuthKeycloakDemo.exchange(s.code(), "x".repeat(43))).error());
        OAuthKeycloakDemo.Started t = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        assertTrue(app.token(OAuthKeycloakDemo.exchange(t.code(), t.verifier())).ok());
    }

    @Test
    void noChallengeAndPlainChallengeAreRefusedAtTheAuthorizationEndpoint() throws Exception {
        assertEquals("invalid_request: Missing parameter: code_challenge_method",
                app.signIn(OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, OAuthKeycloakDemo.ACCOUNTS_READ, "s", null), "ana", "demo-password").error());
        Map<String, String> plain = OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, OAuthKeycloakDemo.ACCOUNTS_READ, "s", "p".repeat(43));
        plain.put("code_challenge_method", "plain");
        assertTrue(app.signIn(plain, "ana", "demo-password").error().startsWith("invalid_request: Invalid parameter: code challenge method"));
    }

    @Test
    void onlyTheExactRegisteredRedirectAddressGetsACode() throws Exception {
        for (String redirect : new String[] {OAuthClient.REDIRECT + "/../steal", "https://budget.example.evil.test/callback", OAuthClient.REDIRECT + "?next=evil",
                "https://BUDGET.example/callback", OAuthClient.REDIRECT + "/", "http://budget.example/callback"}) {
            OAuthClient.Outcome o = app.signIn(OAuthClient.authorizationRequest("budget-app", redirect, OAuthKeycloakDemo.ACCOUNTS_READ, "s", OAuthClient.challenge("v".repeat(43))), "ana", "demo-password");
            assertFalse(o.redirected(), redirect);
            assertEquals("Invalid parameter: redirect_uri", o.errorPage(), redirect);
        }
    }

    @Test
    void theRedirectAddressAtTheTokenEndpointMustMatchToo() throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        Map<String, String> form = OAuthKeycloakDemo.exchange(s.code(), s.verifier());
        form.put("redirect_uri", "https://budget.example/other");
        assertEquals("invalid_grant: Incorrect redirect_uri", app.token(form).error());
    }

    @Test
    void anUnregisteredScopeIsRefused() throws Exception {
        OAuthClient.Outcome o = app.signIn(OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, "openid accounts.read payments.write", "s", OAuthClient.challenge("v".repeat(43))), "ana", "demo-password");
        assertTrue(o.error().startsWith("invalid_scope"));
    }

    @Test
    void reusingACodeEndsTheSessionAtKeycloakButNotAtTheLocalCheck() throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        String code = s.code();
        String access = app.token(OAuthKeycloakDemo.exchange(code, s.verifier())).string("access_token");
        assertEquals(200, app.userinfo(access));
        assertEquals("invalid_grant: Code not valid", app.token(OAuthKeycloakDemo.exchange(code, s.verifier())).error());
        assertEquals(401, app.userinfo(access));
        assertEquals(200, api.get("/accounts", access).status());
    }

    @Test
    void aCodeExpiresAfterFiveSeconds() throws Exception {
        OAuthKeycloakDemo.Started s = OAuthKeycloakDemo.start(app, "ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        Thread.sleep(6_000);
        assertEquals("invalid_grant: Code not valid", app.token(OAuthKeycloakDemo.exchange(s.code(), s.verifier())).error());
    }

    @Test
    void refreshTokensRotateAndAReusedOneEndsTheSession() throws Exception {
        OAuthClient.TokenReply t = tokens("ana", OAuthKeycloakDemo.ACCOUNTS_READ);
        Map<String, String> first = new LinkedHashMap<>(Map.of("grant_type", "refresh_token", "client_id", "budget-app", "refresh_token", t.string("refresh_token")));
        OAuthClient.TokenReply renewed = app.token(first);
        assertTrue(renewed.ok());
        assertNotEquals(t.string("refresh_token"), renewed.string("refresh_token"));
        assertEquals("invalid_grant: Maximum allowed refresh token reuse exceeded", app.token(first).error());
        Map<String, String> second = new LinkedHashMap<>(Map.of("grant_type", "refresh_token", "client_id", "budget-app", "refresh_token", renewed.string("refresh_token")));
        assertFalse(app.token(second).ok());
        assertEquals(401, app.userinfo(renewed.string("access_token")));
    }

    @Test
    void theImplicitFlowAndThePasswordGrantAreOff() throws Exception {
        Map<String, String> implicit = OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, "openid", "s", null);
        implicit.put("response_type", "token");
        assertTrue(app.signIn(implicit, "ana", "demo-password").error().startsWith("unauthorized_client"));
        assertEquals("unauthorized_client: Client not allowed for direct access grants",
                app.token(Map.of("grant_type", "password", "client_id", "budget-app", "username", "ana", "password", "demo-password")).error());
    }

    @Test
    void aConfidentialClientNeedsItsSecretAndStillNeedsPkce() throws Exception {
        String verifier = OAuthClient.newVerifier();
        Map<String, String> request = OAuthClient.authorizationRequest("statements-service", OAuthKeycloakDemo.STATEMENTS_REDIRECT, OAuthKeycloakDemo.ACCOUNTS_READ, "s", OAuthClient.challenge(verifier));
        String code = OAuthClient.codeIfStateMatches(app.signIn(request, "ana", "demo-password"), "s");
        Map<String, String> form = new LinkedHashMap<>(Map.of("grant_type", "authorization_code", "client_id", "statements-service", "code", code,
                "redirect_uri", OAuthKeycloakDemo.STATEMENTS_REDIRECT, "client_secret", "wrong", "code_verifier", verifier));
        assertEquals(401, app.token(form).status());
        form.put("client_secret", "demo-statements-secret");
        form.remove("code_verifier");
        assertEquals("invalid_grant: PKCE code verifier not specified", app.token(form).error());
        assertNull(app.token(form).string("access_token"));
    }
}
