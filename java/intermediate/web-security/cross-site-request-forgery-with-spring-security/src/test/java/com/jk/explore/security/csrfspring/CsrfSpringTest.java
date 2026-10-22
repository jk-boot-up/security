package com.jk.explore.security.csrfspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real Spring Boot servers on the loopback interface, one per mode. */
class CsrfSpringTest {

    @Test
    void withCsrfOffAForgedPostMovesMoney() throws Exception {
        try (var bank = CsrfSpringDemo.start("csrf-off")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana");
            assertEquals(200, ana.postFromAnotherSite("/transfer", CsrfSpringDemo.FORGED).status());
            assertEquals(500, bank.balance("mallory"));
        }
    }

    @Test
    void withTheDefaultAForgedPostIsRefusedAndTheBanksOwnFormWorks() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            assertEquals(403, ana.postFromAnotherSite("/transfer", CsrfSpringDemo.FORGED).status());
            assertEquals(0, bank.balance("mallory"));
            assertEquals(200, ana.post("/transfer", CsrfSpringDemo.HONEST, "X-CSRF-TOKEN", ana.formToken()).status());
            assertEquals(20, bank.balance("mallory"));
        }
    }

    @Test
    void aTokenFromAnotherSessionIsRefused() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            Browser mallory = new Browser(bank.port());
            mallory.signIn("mallory", "X-CSRF-TOKEN", mallory.formToken());
            assertEquals(403, ana.postFromAnotherSite("/transfer", CsrfSpringDemo.FORGED, "X-CSRF-TOKEN", mallory.formToken()).status());
        }
    }

    @Test
    void theSignInFormAndLogoutNeedTokensToo() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            assertEquals(403, ana.signIn("ana").status());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            assertEquals(403, ana.post("/logout", Map.of()).status());
        }
    }

    @Test
    void theTokenIsMaskedAndTheOldOneStopsWorkingAfterSignIn() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            String before = ana.formToken();
            ana.signIn("ana", "X-CSRF-TOKEN", before);
            assertNotEquals(ana.formToken(), ana.formToken());
            assertEquals(403, ana.post("/transfer", CsrfSpringDemo.HONEST, "X-CSRF-TOKEN", before).status());
        }
    }

    @Test
    void singlePageModeUsesAReadableCookieAndAHeader() throws Exception {
        try (var bank = CsrfSpringDemo.start("spa")) {
            Browser ana = new Browser(bank.port());
            ana.get("/csrf");
            ana.signIn("ana", "X-XSRF-TOKEN", ana.cookie("XSRF-TOKEN").getValue());
            ana.get("/csrf");
            assertFalse(ana.cookie("XSRF-TOKEN").isHttpOnly());
            assertEquals(403, ana.postFromAnotherSite("/transfer", CsrfSpringDemo.FORGED).status());
            assertEquals(200, ana.post("/transfer", CsrfSpringDemo.HONEST, "X-XSRF-TOKEN", ana.cookie("XSRF-TOKEN").getValue()).status());
        }
    }

    @Test
    void getIsNeverCheckedSoAStateChangingGetIsForgeable() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            assertEquals(200, ana.getFromAnotherSite("/legacy/move?to=mallory&amount=100").status());
            assertEquals(405, ana.getFromAnotherSite("/transfer?to=mallory&amount=100").status());
            assertEquals(100, bank.balance("mallory"));
        }
    }

    @Test
    void theWebhookIsExemptButChecksItsOwnSignature() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser anyone = new Browser(bank.port());
            assertEquals(401, anyone.postFromAnotherSite("/webhooks/payments", Map.of()).status());
            assertEquals(200, anyone.post("/webhooks/payments", Map.of(), "X-Signature", BankController.WEBHOOK_SECRET).status());
        }
    }

    @Test
    void anExtraExemptionUndoesTheDefence() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer", "csrf.ignore=/webhooks/**,/transfer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            assertEquals(200, ana.postFromAnotherSite("/transfer", CsrfSpringDemo.FORGED).status());
        }
    }

    @Test
    void theApiChainIgnoresCookiesAndWantsTheHeader() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken());
            assertEquals(401, ana.postFromAnotherSite("/api/transfer", CsrfSpringDemo.FORGED).status());
            assertEquals(200, Browser.apiPost(bank.port(), "ana", "/api/transfer", CsrfSpringDemo.HONEST).status());
            assertEquals(401, Browser.apiPost(bank.port(), null, "/api/transfer", CsrfSpringDemo.HONEST).status());
        }
    }

    @Test
    void sameSiteIsOneMoreLayerThatIsOffByDefault() throws Exception {
        try (var bank = CsrfSpringDemo.start("synchronizer")) {
            Browser ana = new Browser(bank.port());
            assertFalse(String.join("|", ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies()).contains("SameSite"));
        }
        try (var bank = CsrfSpringDemo.start("synchronizer", "server.servlet.session.cookie.same-site=lax")) {
            Browser ana = new Browser(bank.port());
            assertTrue(String.join("|", ana.signIn("ana", "X-CSRF-TOKEN", ana.formToken()).setCookies()).contains("SameSite=Lax"));
        }
    }
}
