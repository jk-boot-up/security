package com.jk.explore.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The same flow over a real HTTP server on the loopback interface. */
class LoopbackTest {

    @Test
    void theWholeFlowWorksOverHttp() throws Exception {
        AuthorizationServer server = OAuthDemo.bank(new SimClock(OAuthDemo.START));
        try (LoopbackBank http = new LoopbackBank(server, new BankApi(server))) {
            BudgetApp app = new BudgetApp(http::token);
            HttpResponse<String> redirect = http.authorize(app.startLogin("ana", BankApi.READ), "ana", true);
            assertEquals(302, redirect.statusCode());
            assertTrue(redirect.headers().firstValue("Location").orElseThrow().startsWith(BudgetApp.REDIRECT_URI + "?code="));
            AuthorizationServer.Tokens tokens = app.callback("ana", LoopbackBank.locationQuery(redirect));
            assertEquals(200, http.accounts(tokens.accessToken()));
            assertEquals(401, http.accounts("nonsense"));
        }
    }

    @Test
    void aWrongRedirectIsAnErrorPageAndATokenErrorIsJson() throws Exception {
        AuthorizationServer server = OAuthDemo.bank(new SimClock(OAuthDemo.START));
        try (LoopbackBank http = new LoopbackBank(server, new BankApi(server))) {
            BudgetApp app = new BudgetApp(http::token);
            String url = app.startLogin("ana", BankApi.READ).replace("budget.example%2Fcallback", "evil.example%2Fcallback");
            HttpResponse<String> page = http.authorize(url, "ana", true);
            assertEquals(400, page.statusCode());
            assertTrue(page.headers().firstValue("Location").isEmpty());
            assertEquals("invalid_grant", assertThrows(OAuthException.class, () -> http.token(Map.of("grant_type", "authorization_code",
                    "code", "x", "client_id", BudgetApp.CLIENT_ID, "redirect_uri", BudgetApp.REDIRECT_URI))).error());
        }
    }
}
