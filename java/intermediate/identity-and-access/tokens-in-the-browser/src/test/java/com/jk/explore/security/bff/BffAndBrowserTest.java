package com.jk.explore.security.bff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BffAndBrowserTest {

    private final SimClock clock = new SimClock(BffDemo.START);
    private final AuthServer auth = new AuthServer(clock, BffDemo.seeded("bank signing key"));
    private final BankApi api = new BankApi(clock, auth.publicKey());
    private final Bff bff = new Bff(auth, api, clock);

    private Browser signedIn(Browser.Kind kind) {
        Browser browser = new Browser(kind, bff, api);
        browser.receiveCookie(bff.signIn("ana"));
        return browser;
    }

    @Test
    void theBrowserHoldsOnlyAProtectedCookie() {
        Browser browser = signedIn(Browser.Kind.MODERN);
        Cookie cookie = browser.cookie(Bff.COOKIE);
        assertTrue(cookie.httpOnly() && cookie.secure() && cookie.hostPrefixValid());
        assertEquals("Strict", cookie.sameSite());
        assertTrue(browser.readableByScripts().isEmpty());
    }

    @Test
    void scriptsReadLocalStorageAndPlainCookiesButNotHttpOnlyOnes() {
        Browser browser = new Browser(Browser.Kind.MODERN, bff, api);
        browser.store("access_token", "a");
        browser.receiveCookie("theme=dark; Path=/");
        browser.receiveCookie("__Host-bff=s; Path=/; Secure; HttpOnly; SameSite=Strict");
        assertEquals(2, browser.readableByScripts().size());
        assertEquals(2, new PageScript().collect(browser));
    }

    @Test
    void theBffRefusesRequestsWithoutASessionOrWithoutItsHeader() {
        Browser browser = signedIn(Browser.Kind.MODERN);
        assertEquals(200, browser.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS)).status());
        assertEquals(403, browser.send(Browser.BANK_SITE, Request.of("POST", Bff.TRANSFERS)).status());
        assertEquals(201, browser.send(Browser.BANK_SITE, Request.of("POST", Bff.TRANSFERS).withHeader(Bff.REQUEST_HEADER, "1")).status());
        assertEquals(401, new Browser(Browser.Kind.MODERN, bff, api).send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS)).status());
    }

    @Test
    void sameSiteAndThePreflightStopOtherSites() {
        Browser modern = signedIn(Browser.Kind.MODERN);
        Browser older = signedIn(Browser.Kind.OLDER);
        Request transfer = Request.of("POST", Bff.TRANSFERS);
        assertEquals(401, modern.send("evil.example", transfer).status());
        assertEquals(403, older.send("evil.example", transfer).status());
        assertEquals(0, older.send("evil.example", transfer.withHeader(Bff.REQUEST_HEADER, "1")).status());
        assertEquals(0, api.transfersMade());
    }

    @Test
    void theBffRefreshesOnTheServerWhenTheAccessTokenExpires() {
        Browser browser = signedIn(Browser.Kind.MODERN);
        clock.advance(AuthServer.ACCESS_SECONDS + 1);
        assertEquals(200, browser.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS)).status());
        clock.advance(AuthServer.REFRESH_SECONDS + 1);
        assertEquals(401, browser.send(Browser.BANK_SITE, Request.of("GET", Bff.ACCOUNTS)).status());
    }

    @Test
    void aScriptCanActOnlyWhileThePageIsOpen() {
        Browser browser = signedIn(Browser.Kind.MODERN);
        PageScript script = new PageScript();
        assertEquals(200, script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS)).status());
        browser.closePage();
        assertEquals(0, script.requestFromPage(browser, Request.of("GET", Bff.ACCOUNTS)).status());
    }

    @Test
    void aCookieWithADomainBreaksTheHostPrefix() {
        assertFalse(Cookie.parse("__Host-bff=s; Path=/; Secure; Domain=bank.example").hostPrefixValid());
        assertFalse(Cookie.parse("__Host-bff=s; Path=/app; Secure").hostPrefixValid());
        assertFalse(Cookie.parse("__Host-bff=s; Path=/").hostPrefixValid());
    }
}
