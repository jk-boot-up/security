package com.jk.explore.security.oauthkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** How the demo prints addresses and refusals, checked without Keycloak. */
class ShownTest {

    @Test
    void anAddressIsPrintedOneParameterPerLineWithRandomValuesHidden() {
        StringBuilder out = new StringBuilder();
        OAuthKeycloakDemo.url(out, "  ", "302 Location: ",
                "https://budget.example/callback?state=abc&session_state=s1&iss=http%3A%2F%2F127.0.0.1%3A5555%2Frealms%2Fbank&code=xyz", "http://127.0.0.1:5555");
        assertEquals("""
                  302 Location: https://budget.example/callback
                    ?state=<state>
                    &session_state=<session>
                    &iss=<keycloak>/realms/bank
                    &code=<code>
                """, out.toString());
    }

    @Test
    void aRefusalIsTheErrorThenItsReasonWrappedToFit() {
        StringBuilder out = new StringBuilder();
        OAuthKeycloakDemo.refused(out, "    ", "unauthorized_client: Client is not allowed to initiate browser login with given response_type. "
                + "Implicit flow is disabled for the client.");
        List<String> lines = out.toString().lines().toList();
        assertEquals("    -> refused: unauthorized_client", lines.get(0));
        assertTrue(lines.size() > 2);
        assertTrue(lines.stream().allMatch(l -> l.length() <= 70), out.toString());
        assertEquals("Client is not allowed to initiate browser login with given response_type. Implicit flow is disabled for the client.",
                String.join(" ", lines.subList(1, lines.size()).stream().map(String::strip).toList()));
    }

    @Test
    void anAcceptedRequestIsShownAsTokensIssued() {
        StringBuilder out = new StringBuilder();
        OAuthKeycloakDemo.refused(out, "  ", null);
        assertEquals("  -> tokens issued\n", out.toString());
    }
}
