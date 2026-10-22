package com.jk.explore.security.oauthkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The parts of the app that need no server. */
class PkceTest {

    @Test
    void theRfc7636ExampleGivesThePublishedChallenge() {
        assertEquals(OAuthKeycloakDemo.RFC_CHALLENGE, OAuthClient.challenge(OAuthKeycloakDemo.RFC_VERIFIER));
    }

    @Test
    void aNewVerifierIsLongRandomAndUrlSafe() {
        String a = OAuthClient.newVerifier();
        String b = OAuthClient.newVerifier();
        assertNotEquals(a, b);
        assertEquals(43, a.length());
        assertTrue(a.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void theChallengeIsNotTheVerifier() {
        String v = OAuthClient.newVerifier();
        assertNotEquals(v, OAuthClient.challenge(v));
        assertEquals(43, OAuthClient.challenge(v).length());
    }

    @Test
    void theAuthorizationRequestCarriesTheChallengeAndNotTheVerifier() {
        String v = OAuthClient.newVerifier();
        Map<String, String> r = OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, "openid accounts.read", "s", OAuthClient.challenge(v));
        assertEquals("S256", r.get("code_challenge_method"));
        assertEquals("code", r.get("response_type"));
        assertFalse(r.containsValue(v));
        assertFalse(OAuthClient.authorizationRequest("budget-app", OAuthClient.REDIRECT, "openid", "s", null).containsKey("code_challenge"));
    }

    @Test
    void theStateOfAnotherSignInIsRefused() {
        OAuthClient.Outcome o = new OAuthClient.Outcome(302, OAuthClient.REDIRECT + "?state=mallory&code=abc", null, false);
        assertEquals("abc", OAuthClient.codeIfStateMatches(o, "mallory"));
        assertEquals("invalid_state: this browser did not start this sign-in", assertThrows(IllegalStateException.class, () -> OAuthClient.codeIfStateMatches(o, "ana")).getMessage());
    }

    @Test
    void anErrorRedirectIsParsedFromTheQueryAndFromTheFragment() {
        assertEquals("invalid_request: Missing parameter", new OAuthClient.Outcome(302, OAuthClient.REDIRECT + "?error=invalid_request&error_description=Missing+parameter&state=s", null, false).error());
        assertEquals("unauthorized_client: nope", new OAuthClient.Outcome(302, OAuthClient.REDIRECT + "#error=unauthorized_client&error_description=nope", null, false).error());
    }
}
