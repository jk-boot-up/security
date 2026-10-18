package com.jk.explore.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthorizationServerTest {

    private final SimClock clock = new SimClock(OAuthDemo.START);
    private final AuthorizationServer server = OAuthDemo.bank(clock);
    private final BankApi api = new BankApi(server);

    private Map<String, String> request(String verifier) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("response_type", "code");
        p.put("client_id", BudgetApp.CLIENT_ID);
        p.put("redirect_uri", BudgetApp.REDIRECT_URI);
        p.put("scope", BankApi.READ);
        p.put("state", "s1");
        p.put("code_challenge", Pkce.challenge(verifier));
        p.put("code_challenge_method", Pkce.S256);
        return p;
    }

    private String code(String verifier) {
        AuthorizationServer.AuthorizeResult r = server.authorize(request(verifier), "ana", true);
        assertTrue(r.redirected());
        assertEquals("s1", Urls.queryOf(r.redirectTo()).get("state"));
        return Urls.queryOf(r.redirectTo()).get("code");
    }

    private Map<String, String> redeemForm(String code, String verifier) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("grant_type", "authorization_code");
        f.put("code", code);
        f.put("redirect_uri", BudgetApp.REDIRECT_URI);
        f.put("client_id", BudgetApp.CLIENT_ID);
        f.put("code_verifier", verifier);
        return f;
    }

    private String error(Map<String, String> form) {
        return assertThrows(OAuthException.class, () -> server.token(form)).error();
    }

    @Test
    void theRightVerifierRedeemsTheCodeForAWorkingToken() throws Exception {
        String v = Pkce.newVerifier();
        AuthorizationServer.Tokens t = server.token(redeemForm(code(v), v));
        assertEquals(Set.of(BankApi.READ), t.scope());
        assertEquals(200, api.call("GET", "/accounts", "Bearer " + t.accessToken()).status());
    }

    @Test
    void aWrongOrMissingVerifierIsRefused() {
        String v = Pkce.newVerifier();
        String c = code(v);
        assertEquals("invalid_grant", error(redeemForm(c, Pkce.newVerifier())));
        Map<String, String> none = redeemForm(c, v);
        none.remove("code_verifier");
        assertEquals("invalid_grant", error(none));
    }

    @Test
    void aFailedAttemptDoesNotBurnTheCodeForTheRealClient() throws Exception {
        String v = Pkce.newVerifier();
        String c = code(v);
        error(redeemForm(c, Pkce.newVerifier()));
        server.token(redeemForm(c, v));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://budget.example/callback/", "https://budget.example/callback/../x", "https://budget.example.evil.test/callback",
            "https://budget.example/callback?x=1", "https://BUDGET.example/callback", "http://budget.example/callback"})
    void onlyTheExactRegisteredRedirectUriIsAccepted(String uri) {
        Map<String, String> p = request(Pkce.newVerifier());
        p.put("redirect_uri", uri);
        AuthorizationServer.AuthorizeResult r = server.authorize(p, "ana", true);
        assertFalse(r.redirected());
        assertNull(r.redirectTo());
    }

    @Test
    void unknownClientsAreShownAnErrorAtTheBank() {
        Map<String, String> p = request(Pkce.newVerifier());
        p.put("client_id", "nobody");
        assertFalse(server.authorize(p, "ana", true).redirected());
    }

    @Test
    void plainMissingChallengeImplicitScopeAndRefusalAreErrorsSentBack() {
        Map<String, String> plain = request(Pkce.newVerifier());
        plain.put("code_challenge_method", "plain");
        assertEquals("invalid_request", errorBack(server.authorize(plain, "ana", true)));
        Map<String, String> none = request(Pkce.newVerifier());
        none.remove("code_challenge");
        assertEquals("invalid_request", errorBack(server.authorize(none, "ana", true)));
        Map<String, String> implicit = request(Pkce.newVerifier());
        implicit.put("response_type", "token");
        assertEquals("unsupported_response_type", errorBack(server.authorize(implicit, "ana", true)));
        Map<String, String> greedy = request(Pkce.newVerifier());
        greedy.put("scope", BankApi.READ + " " + BankApi.PAY);
        assertEquals("invalid_scope", errorBack(server.authorize(greedy, "ana", true)));
        assertEquals("access_denied", errorBack(server.authorize(request(Pkce.newVerifier()), "ana", false)));
    }

    private static String errorBack(AuthorizationServer.AuthorizeResult r) {
        assertTrue(r.redirected());
        Map<String, String> q = Urls.queryOf(r.redirectTo());
        assertEquals("s1", q.get("state"));
        assertFalse(q.containsKey("code"));
        return q.get("error");
    }

    @Test
    void aCodeWorksOnceAndASecondUseRevokesWhatTheFirstIssued() throws Exception {
        String v = Pkce.newVerifier();
        String c = code(v);
        AuthorizationServer.Tokens t = server.token(redeemForm(c, v));
        assertEquals("invalid_grant", error(redeemForm(c, v)));
        assertEquals(401, api.call("GET", "/accounts", "Bearer " + t.accessToken()).status());
        assertEquals("invalid_grant", error(Map.of("grant_type", "refresh_token", "refresh_token", t.refreshToken(), "client_id", BudgetApp.CLIENT_ID)));
    }

    @Test
    void aCodeExpiresAfterSixtySeconds() throws Exception {
        String v = Pkce.newVerifier();
        String c = code(v);
        clock.advance(AuthorizationServer.CODE_LIFETIME);
        String c2 = code(v);
        clock.advance(1);
        assertEquals("invalid_grant", error(redeemForm(c, v)));
        server.token(redeemForm(c2, v));
    }

    @Test
    void theRedirectUriMustMatchTheOneInTheRequest() {
        String v = Pkce.newVerifier();
        Map<String, String> f = redeemForm(code(v), v);
        f.put("redirect_uri", "https://budget.example/other");
        assertEquals("invalid_grant", error(f));
    }

    @Test
    void aCodeIssuedToOneClientCannotBeRedeemedByAnother() {
        String v = Pkce.newVerifier();
        Map<String, String> f = redeemForm(code(v), v);
        f.put("client_id", OAuthDemo.STATEMENTS_ID);
        f.put("client_secret", OAuthDemo.STATEMENTS_SECRET);
        assertEquals("invalid_grant", error(f));
    }

    @Test
    void refreshTokensRotateAndAReuseRevokesTheFamily() throws Exception {
        String v = Pkce.newVerifier();
        AuthorizationServer.Tokens first = server.token(redeemForm(code(v), v));
        AuthorizationServer.Tokens second = server.token(Map.of("grant_type", "refresh_token", "refresh_token", first.refreshToken(), "client_id", BudgetApp.CLIENT_ID));
        assertEquals(200, api.call("GET", "/accounts", "Bearer " + second.accessToken()).status());
        assertEquals("invalid_grant", error(Map.of("grant_type", "refresh_token", "refresh_token", first.refreshToken(), "client_id", BudgetApp.CLIENT_ID)));
        assertEquals("invalid_grant", error(Map.of("grant_type", "refresh_token", "refresh_token", second.refreshToken(), "client_id", BudgetApp.CLIENT_ID)));
        assertEquals(401, api.call("GET", "/accounts", "Bearer " + second.accessToken()).status());
    }

    @Test
    void accessTokensExpireAndScopesLimitWhatTheyDo() throws Exception {
        String v = Pkce.newVerifier();
        AuthorizationServer.Tokens t = server.token(redeemForm(code(v), v));
        assertEquals(403, api.call("POST", "/payments", "Bearer " + t.accessToken()).status());
        assertEquals(401, api.call("GET", "/accounts", null).status());
        assertEquals(401, api.call("GET", "/accounts", "Bearer nonsense").status());
        clock.advance(AuthorizationServer.ACCESS_LIFETIME - 1);
        assertEquals(200, api.call("GET", "/accounts", "Bearer " + t.accessToken()).status());
        clock.advance(1);
        assertEquals(401, api.call("GET", "/accounts", "Bearer " + t.accessToken()).status());
    }

    @Test
    void thePasswordGrantAndUnknownGrantsAreNotOffered() {
        assertEquals("unsupported_grant_type", error(Map.of("grant_type", "password", "client_id", BudgetApp.CLIENT_ID, "username", "ana", "password", "x")));
        assertEquals("unsupported_grant_type", error(Map.of("grant_type", "client_credentials", "client_id", BudgetApp.CLIENT_ID)));
    }

    @Test
    void confidentialClientsMustAuthenticate() {
        Map<String, String> f = new LinkedHashMap<>(Map.of("grant_type", "authorization_code", "code", "x", "client_id", OAuthDemo.STATEMENTS_ID));
        assertEquals("invalid_client", error(f));
        f.put("client_secret", "wrong");
        assertEquals("invalid_client", error(f));
        f.put("client_secret", OAuthDemo.STATEMENTS_SECRET);
        assertEquals("invalid_grant", error(f));
        assertEquals("invalid_client", error(Map.of("grant_type", "authorization_code", "client_id", "nobody")));
    }
}
