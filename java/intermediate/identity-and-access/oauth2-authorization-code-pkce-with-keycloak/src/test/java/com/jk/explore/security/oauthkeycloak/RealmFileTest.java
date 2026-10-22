package com.jk.explore.security.oauthkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** The settings that make the lesson true are in the realm file, so they are checked without starting Keycloak. */
class RealmFileTest {

    private static Map<String, Object> realm;

    @BeforeAll
    static void read() throws Exception {
        realm = JsonMapper.builder().build().readValue(Files.readString(Path.of("infra", "bank-realm.json")), new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> client(String id) {
        return ((List<Map<String, Object>>) realm.get("clients")).stream().filter(c -> id.equals(c.get("clientId"))).findFirst().orElseThrow();
    }

    @Test
    void theRealmIsTheBankRealmWithShortLivedTokensAndCodes() {
        assertEquals("bank", realm.get("realm"));
        assertEquals(300, realm.get("accessTokenLifespan"));
        assertEquals(5, realm.get("accessCodeLifespan"));
        assertEquals("external", realm.get("sslRequired"));
    }

    @Test
    void refreshTokensAreRotatedAndNeverReusable() {
        assertEquals(true, realm.get("revokeRefreshToken"));
        assertEquals(0, realm.get("refreshTokenMaxReuse"));
    }

    @Test
    void bothClientsRequireS256Pkce() {
        for (String id : List.of("budget-app", "statements-service")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) client(id).get("attributes");
            assertEquals("S256", attributes.get("pkce.code.challenge.method"), id);
        }
    }

    @Test
    void neitherClientAllowsTheImplicitFlowOrThePasswordGrantOrServiceAccounts() {
        for (String id : List.of("budget-app", "statements-service")) {
            assertEquals(false, client(id).get("implicitFlowEnabled"), id);
            assertEquals(false, client(id).get("directAccessGrantsEnabled"), id);
            assertEquals(false, client(id).get("serviceAccountsEnabled"), id);
        }
    }

    @Test
    void redirectAddressesAreExactAndNeverWildcards() {
        for (String id : List.of("budget-app", "statements-service")) {
            @SuppressWarnings("unchecked")
            List<String> uris = (List<String>) client(id).get("redirectUris");
            assertEquals(1, uris.size(), id);
            assertTrue(uris.get(0).startsWith("https://"), id);
            assertFalse(uris.get(0).contains("*"), id);
        }
    }

    @Test
    void thePhoneAppIsPublicAndTheStatementsServiceIsConfidential() {
        assertEquals(true, client("budget-app").get("publicClient"));
        assertFalse(client("budget-app").containsKey("secret"));
        assertEquals(false, client("statements-service").get("publicClient"));
        assertTrue(client("statements-service").containsKey("secret"));
    }

    @Test
    void theBudgetAppMustBeAskedForConsentAndGetsOnlyAccountsRead() {
        assertEquals(true, client("budget-app").get("consentRequired"));
        assertEquals(List.of("accounts.read"), client("budget-app").get("defaultClientScopes"));
        assertEquals(List.of(), client("budget-app").get("optionalClientScopes"));
    }
}
