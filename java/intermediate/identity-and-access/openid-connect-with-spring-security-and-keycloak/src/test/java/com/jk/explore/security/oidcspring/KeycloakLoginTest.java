package com.jk.explore.security.oidcspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real Keycloak in Docker and the real store. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeycloakLoginTest {

    private KeycloakLab keycloak;
    private Running store;
    private Tokens tokens;

    @BeforeAll
    void start() throws Exception {
        int port = Running.freePort();
        keycloak = KeycloakLab.start(port);
        store = Running.start(keycloak, port);
        tokens = new Tokens(keycloak.issuer("bank"), store.url());
    }

    @AfterAll
    void stop() {
        store.close();
        keycloak.close();
    }

    private Shopper signedIn(String registration, String user) throws Exception {
        Shopper s = new Shopper();
        s.navigate(store.url() + "/oauth2/authorization/" + registration, user, "demo-password");
        return s;
    }

    private Map<String, Object> me(Shopper s) throws Exception {
        return OidcSpringDemo.json(s.get(store.url() + "/me").body());
    }

    @Test
    void anonymousVisitorsAreRefusedAndTheHomePageIsOpen() throws Exception {
        Shopper s = new Shopper();
        assertEquals(401, s.get(store.url() + "/me").status());
        assertEquals(200, s.get(store.url() + "/").status());
    }

    @Test
    void theAuthorizationRequestCarriesStateNonceAndPkce() throws Exception {
        String location = new Shopper().once(store.url() + "/oauth2/authorization/bank").headers().firstValue("Location").orElseThrow();
        assertTrue(location.contains("scope=openid%20email"));
        assertTrue(location.contains("state=") && location.contains("nonce=") && location.contains("code_challenge_method=S256"));
    }

    @Test
    void signingInGivesTheIssuerAndSubjectAndAVerifiedEmail() throws Exception {
        Map<String, Object> me = me(signedIn("bank", "ana"));
        assertEquals(keycloak.issuer("bank"), me.get("issuer"));
        assertEquals(AccountsKeys.ANA, me.get("subject"));
        assertEquals("ana@mail.example", me.get("email"));
        assertEquals(true, me.get("emailVerified"));
    }

    @Test
    void aWrongPasswordDoesNotSignAnyoneIn() throws Exception {
        Shopper s = new Shopper();
        s.navigate(store.url() + "/oauth2/authorization/bank", "ana", "wrong-password");
        assertEquals(401, s.get(store.url() + "/me").status());
    }

    @Test
    void theAccessTokenIsNotAnIdToken() throws Exception {
        Tokens.Reply r = tokens.signIn("bank", "ana", "openid email");
        assertEquals("Bearer", Tokens.claims(r.string("access_token")).get("typ"));
        assertEquals("ID", Tokens.claims(r.string("id_token")).get("typ"));
        assertFalse(Tokens.claims(r.string("access_token")).containsKey("aud"));
    }

    @Test
    void withoutTheOpenidScopeThereIsNoIdToken() throws Exception {
        assertEquals(null, tokens.signIn("bank", "ana", "email").string("id_token"));
    }

    @Test
    void userinfoAnswersToTheAccessTokenAndNotToTheIdToken() throws Exception {
        Tokens.Reply r = tokens.signIn("bank", "ana", "openid email");
        assertEquals(200, tokens.userinfo(r.string("access_token")).statusCode());
        assertEquals(401, tokens.userinfo(r.string("id_token")).statusCode());
    }

    @Test
    void theWalletUserHasTheSameEmailAddressUnverifiedAndAnotherSubject() throws Exception {
        Map<String, Object> mal = me(signedIn("wallet", "mal"));
        assertEquals("ana@mail.example", mal.get("email"));
        assertEquals(false, mal.get("emailVerified"));
        assertEquals(AccountsKeys.MAL, mal.get("subject"));
        assertNotEquals(AccountsKeys.ANA, mal.get("subject"));
    }

    @Test
    void keyingByEmailGivesMalloryAnasAccountAndKeyingBySubjectDoesNot() throws Exception {
        Shopper mal = signedIn("wallet", "mal");
        assertEquals("account of Ana", OidcSpringDemo.json(mal.get(store.url() + "/account?keyedBy=email").body()).get("account"));
        String bySubject = String.valueOf(OidcSpringDemo.json(mal.get(store.url() + "/account?keyedBy=subject").body()).get("account"));
        assertTrue(bySubject.startsWith("new account for wallet"));
        Shopper ana = signedIn("bank", "ana");
        assertEquals("account of Ana", OidcSpringDemo.json(ana.get(store.url() + "/account?keyedBy=subject").body()).get("account"));
    }

    @Test
    void aCallbackFromAnotherSignInIsRefused() throws Exception {
        Shopper mallory = new Shopper();
        Shopper.Page callback = mallory.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password", store.url() + "/login/oauth2/code/bank");
        Shopper victim = new Shopper();
        victim.once(store.url() + "/oauth2/authorization/bank");
        victim.get(callback.url());
        assertEquals(401, victim.get(store.url() + "/me").status());
    }

    @Test
    void signingOutEndsTheStoreSessionAndTheProviderSession() throws Exception {
        Shopper ana = signedIn("bank", "ana");
        String csrf = String.valueOf(OidcSpringDemo.json(ana.get(store.url() + "/csrf").body()).get("token"));
        Shopper.Page logout = ana.post(store.url() + "/logout", "_csrf=" + Shopper.enc(csrf));
        assertTrue(logout.url().contains("/protocol/openid-connect/logout"));
        assertEquals(401, ana.get(store.url() + "/me").status());
        ana.navigate(logout.url(), null, null);
        assertTrue(ana.get(store.url() + "/oauth2/authorization/bank").body().contains("kc-form-login"), "the provider session is gone, so the password is asked for again");
    }

    @Test
    void aLogoutPostWithoutTheCsrfTokenIsRefused() throws Exception {
        Shopper ana = signedIn("bank", "ana");
        assertEquals(403, ana.post(store.url() + "/logout", "").status());
        assertEquals(200, ana.get(store.url() + "/me").status());
    }

    @Test
    void backChannelLogoutEndsOnlyTheSessionThatKeycloakEnded() throws Exception {
        Shopper laptop = signedIn("bank", "ana");
        Shopper phone = signedIn("bank", "ana");
        assertEquals(204, keycloak.endSession("bank", String.valueOf(me(laptop).get("sid"))));
        boolean out = false;
        for (int i = 0; i < 40 && !out; i++) {
            out = laptop.get(store.url() + "/me").status() == 401;
            if (!out) {
                Thread.sleep(250);
            }
        }
        assertTrue(out);
        assertEquals(200, phone.get(store.url() + "/me").status());
    }

    @Test
    void aStrangersLogoutTokenEndsNothing() throws Exception {
        Shopper phone = signedIn("bank", "ana");
        java.net.http.HttpResponse<String> r = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(store.url() + "/logout/connect/back-channel/bank"))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(java.net.http.HttpRequest.BodyPublishers.ofString("logout_token=not.a.token")).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(400, r.statusCode());
        assertEquals(200, phone.get(store.url() + "/me").status());
    }

    @Test
    void theStoreReloadsKeysWhenKeycloakRotatesThem() throws Exception {
        keycloak.addSigningKey("bank");
        Shopper ana = signedIn("bank", "ana");
        assertEquals(AccountsKeys.ANA, me(ana).get("subject"));
        assertTrue(LocalProvider.kidsOf(keycloak.certs("bank")).size() >= 2);
    }
}
