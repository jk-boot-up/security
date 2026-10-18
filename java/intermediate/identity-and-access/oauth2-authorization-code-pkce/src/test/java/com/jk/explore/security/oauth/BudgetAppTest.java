package com.jk.explore.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetAppTest {

    private final AuthorizationServer server = OAuthDemo.bank(new SimClock(OAuthDemo.START));
    private final BudgetApp app = new BudgetApp(server::token);

    private Map<String, String> signIn(String session, String user) {
        return Urls.queryOf(server.authorize(Urls.queryOf(app.startLogin(session, BankApi.READ)), user, true).redirectTo());
    }

    @Test
    void eachSignInGetsItsOwnStateAndTheRequestCarriesOnlyTheChallenge() {
        String a = app.startLogin("one", BankApi.READ);
        String b = app.startLogin("two", BankApi.READ);
        assertNotEquals(Urls.queryOf(a).get("state"), Urls.queryOf(b).get("state"));
        assertEquals(app.stateFor("one"), Urls.queryOf(a).get("state"));
        String withRfc = app.startLogin("three", BankApi.READ, OAuthDemo.RFC7636_VERIFIER);
        assertTrue(withRfc.contains(OAuthDemo.RFC7636_CHALLENGE));
        assertFalse(withRfc.contains(OAuthDemo.RFC7636_VERIFIER));
    }

    @Test
    void theRightCallbackGivesTokens() throws Exception {
        assertTrue(app.callback("ana", signIn("ana", "ana")).accessToken().length() > 40);
    }

    @Test
    void aCallbackWithAnotherBrowsersStateIsRefused() {
        Map<String, String> mallorys = signIn("mallory", "mallory");
        app.startLogin("ana", BankApi.READ);
        assertEquals("invalid_state", assertThrows(OAuthException.class, () -> app.callback("ana", mallorys)).error());
    }

    @Test
    void aCallbackNobodyStartedIsRefused() {
        Map<String, String> q = new LinkedHashMap<>(Map.of("code", "x", "state", "y"));
        assertEquals("invalid_state", assertThrows(OAuthException.class, () -> app.callback("ana", q)).error());
    }

    @Test
    void aStateWorksOnlyOnce() throws Exception {
        Map<String, String> back = signIn("ana", "ana");
        app.callback("ana", back);
        assertEquals("invalid_state", assertThrows(OAuthException.class, () -> app.callback("ana", back)).error());
    }

    @Test
    void anErrorFromTheBankIsReportedAfterTheStateCheck() {
        Map<String, String> p = new LinkedHashMap<>(Urls.queryOf(app.startLogin("ana", BankApi.READ)));
        Map<String, String> back = Urls.queryOf(server.authorize(p, "ana", false).redirectTo());
        assertEquals("access_denied", assertThrows(OAuthException.class, () -> app.callback("ana", back)).error());
    }
}
