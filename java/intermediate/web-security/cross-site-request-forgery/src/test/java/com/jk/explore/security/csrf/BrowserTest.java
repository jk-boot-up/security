package com.jk.explore.security.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The simulated browser against the real server on this machine: which requests carry the cookie. */
class BrowserTest {

    private static final Map<String, String> FORM = Map.of("to", "mallory", "amount", "5");

    private static int crossSitePost(String sameSite, boolean legacy) throws Exception {
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite(sameSite)))) {
            return CsrfDemo.ana(server, legacy).submit(CsrfDemo.EVIL, "POST", CsrfDemo.TRANSFER, FORM).status();
        }
    }

    private static int crossSiteLink(String sameSite) throws Exception {
        try (BankServer server = new BankServer(new Bank(Protection.none().withSameSite(sameSite)))) {
            return CsrfDemo.ana(server, false).click(CsrfDemo.EVIL, CsrfDemo.BANK + "/account").status();
        }
    }

    @Test
    void crossSitePostsCarryTheCookieOnlyWithSameSiteNone() throws Exception {
        assertEquals(200, crossSitePost("None", false));
        assertEquals(401, crossSitePost("Lax", false));
        assertEquals(401, crossSitePost("Strict", false));
        assertEquals(401, crossSitePost(null, false));
    }

    @Test
    void crossSiteLinksCarryALaxCookieButNotAStrictOne() throws Exception {
        assertEquals(200, crossSiteLink("Lax"));
        assertEquals(401, crossSiteLink("Strict"));
    }

    @Test
    void aLegacyBrowserIgnoresSameSite() throws Exception {
        assertEquals(200, crossSitePost("Strict", true));
    }

    @Test
    void theSessionCookieIsKeptAsSecure() throws Exception {
        try (BankServer server = new BankServer(new Bank(Protection.none()))) {
            Browser browser = new Browser(server, false);
            browser.submit(CsrfDemo.BANK, "POST", CsrfDemo.BANK + "/login", Map.of("user", "ana", "password", "correct horse battery staple"));
            assertTrue(browser.cookie("bank.example", "sid").isPresent());
            assertTrue(browser.cookie("bank.example", "sid").get().secure());
        }
    }

    @Test
    void theBrowserSendsFetchMetadataAndOrigin() throws Exception {
        try (BankServer server = new BankServer(new Bank(Protection.none().withFetchChecks()))) {
            Browser modern = CsrfDemo.ana(server, false);
            assertEquals(403, modern.submit(CsrfDemo.REVIEWS, "POST", CsrfDemo.TRANSFER, FORM).status());
            assertEquals(200, modern.submit(CsrfDemo.BANK, "POST", CsrfDemo.TRANSFER, FORM).status());
            Browser legacy = CsrfDemo.ana(server, true);
            assertEquals(403, legacy.submit(CsrfDemo.EVIL, "POST", CsrfDemo.TRANSFER, FORM).status());
        }
    }

    @Test
    void theTokenTravelsWithTheBanksOwnForm() throws Exception {
        try (BankServer server = new BankServer(new Bank(Protection.none().withTokens()))) {
            Browser ana = CsrfDemo.ana(server, false);
            String token = CsrfDemo.token(ana);
            assertEquals(200, ana.submit(CsrfDemo.BANK, "POST", CsrfDemo.TRANSFER, Map.of("to", "mallory", "amount", "5", "csrf", token)).status());
            assertEquals(995, server.bank().balance("ana"));
        }
    }
}
