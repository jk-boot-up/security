package com.jk.explore.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoreTest {

    @Test
    void theFullFlowSignsAnaIn() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser browser = new Store.Browser("ana");
        Provider.AuthorizationRequest request = store.beginLogin(browser, OidcDemo.BANK, "openid email");
        Store.Account account = store.callback(browser, world.bank.authorize(request, OidcDemo.ANA));
        assertEquals("https://bank.example | ana-0001", account.key());
        assertEquals("ana@mail.example", account.email());
        assertTrue(store.signedIn(browser).isPresent());
    }

    @Test
    void aCallbackWithAnotherBrowsersStateIsRefused() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("ana");
        Store.Browser mallory = new Store.Browser("mallory");
        store.beginLogin(ana, OidcDemo.BANK, "openid");
        Provider.AuthorizationRequest theirs = store.beginLogin(mallory, OidcDemo.BANK, "openid");
        Provider.AuthorizationResponse response = world.bank.authorize(theirs, OidcDemo.ANA);
        assertEquals("state does not match this browser",
                assertThrows(InvalidTokenException.class, () -> store.callback(ana, response)).getMessage());
    }

    @Test
    void aCodeWorksOnce() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Provider.AuthorizationRequest request = store.beginLogin(new Store.Browser("a"), OidcDemo.BANK, "openid");
        Provider.AuthorizationResponse response = world.bank.authorize(request, OidcDemo.ANA);
        world.bank.token(response.code(), Store.CLIENT_ID);
        assertThrows(InvalidTokenException.class, () -> world.bank.token(response.code(), Store.CLIENT_ID));
    }

    @Test
    void aRedirectThatWasNotRegisteredIsRefused() {
        OidcDemo.World world = new OidcDemo.World();
        Provider.AuthorizationRequest request = new Provider.AuthorizationRequest(OidcDemo.BANK, Store.CLIENT_ID,
                "https://shop.example.evil/callback", "openid", "s", "n");
        assertThrows(InvalidTokenException.class, () -> world.bank.authorize(request, OidcDemo.ANA));
    }

    @Test
    void accountsAreKeyedByIssuerAndSubject() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        OidcDemo.login(store, new Store.Browser("a"), world.bank, OidcDemo.ANA, "openid email");
        Store.Browser mallory = new Store.Browser("m");
        OidcDemo.login(store, mallory, world.wallet, OidcDemo.MALLORY, "openid email");
        assertEquals(2, store.accountCount());
        assertNull(store.signedIn(mallory).orElseThrow().email());
    }

    @Test
    void keyingByEmailMergesStrangers() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.EMAIL);
        OidcDemo.login(store, new Store.Browser("a"), world.bank, OidcDemo.ANA, "openid email");
        Store.Browser mallory = new Store.Browser("m");
        OidcDemo.login(store, mallory, world.wallet, OidcDemo.MALLORY, "openid email");
        assertEquals(1, store.accountCount());
        assertEquals(OidcDemo.ANA, store.signedIn(mallory).orElseThrow().subject());
    }

    @Test
    void userinfoNeedsAValidAccessToken() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        OidcDemo.Login login = OidcDemo.login(store, new Store.Browser("a"), world.bank, OidcDemo.ANA, "openid email");
        assertEquals(OidcDemo.ANA, world.bank.userinfo(login.tokens().accessToken()).get("sub"));
        assertThrows(InvalidTokenException.class, () -> world.bank.userinfo(login.tokens().idToken()));
        world.clock.advance(Provider.ACCESS_TOKEN_SECONDS);
        assertThrows(InvalidTokenException.class, () -> world.bank.userinfo(login.tokens().accessToken()));
    }

    @Test
    void logoutEndsTheStoreSessionAndCarriesTheHint() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser ana = new Store.Browser("a");
        OidcDemo.login(store, ana, world.bank, OidcDemo.ANA, "openid");
        String redirect = store.logout(ana);
        assertFalse(store.signedIn(ana).isPresent());
        assertTrue(redirect.startsWith("https://bank.example/logout?"));
        assertEquals(List.of("id_token_hint", "post_logout_redirect_uri", "state"), Store.parameterNames(redirect));
    }

    @Test
    void backChannelLogoutEndsOnlyTheNamedSession() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Store.Browser laptop = new Store.Browser("l");
        Store.Browser phone = new Store.Browser("p");
        OidcDemo.Login first = OidcDemo.login(store, laptop, world.bank, OidcDemo.ANA, "openid");
        OidcDemo.login(store, phone, world.bank, OidcDemo.ANA, "openid");
        String sid = (String) OidcDemo.claimsOf(first.tokens().idToken()).get("sid");
        assertEquals(1, store.backchannelLogout(world.bank.logoutToken(Store.CLIENT_ID, OidcDemo.ANA, sid)));
        assertFalse(store.signedIn(laptop).isPresent());
        assertTrue(store.signedIn(phone).isPresent());
    }

    @Test
    void badLogoutTokensAreRefused() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Store store = world.store(Store.AccountKey.ISSUER_AND_SUBJECT);
        Map<String, Object> claims = OidcDemo.claimsOf(world.bank.logoutToken(Store.CLIENT_ID, OidcDemo.ANA, "sid-x"));
        assertEquals("a logout token must not have a nonce", assertThrows(InvalidTokenException.class,
                () -> store.backchannelLogout(world.bank.sign("logout+jwt", OidcDemo.with(claims, "nonce", "n")))).getMessage());
        Map<String, Object> noEvent = new LinkedHashMap<>(claims);
        noEvent.remove("events");
        assertEquals("no logout event", assertThrows(InvalidTokenException.class,
                () -> store.backchannelLogout(world.bank.sign("logout+jwt", noEvent))).getMessage());
        assertEquals("not a logout token", assertThrows(InvalidTokenException.class,
                () -> store.backchannelLogout(world.bank.sign("JWT", claims))).getMessage());
        assertEquals("audience does not include this client", assertThrows(InvalidTokenException.class,
                () -> store.backchannelLogout(world.bank.sign("logout+jwt", OidcDemo.with(claims, "aud", "budget-app")))).getMessage());
        assertThrows(InvalidTokenException.class,
                () -> store.backchannelLogout(Provider.signWith("bank-2026-a", OidcDemo.STRANGER_KEY, "logout+jwt", claims)));
    }
}
