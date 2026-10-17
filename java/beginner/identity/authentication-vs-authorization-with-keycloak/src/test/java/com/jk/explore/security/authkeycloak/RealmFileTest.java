package com.jk.explore.security.authkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The realm that is imported into Keycloak is configuration, and configuration is checked like code. */
class RealmFileTest {

    private final JsonNode realm = JsonMapper.builder().build().readTree(Files.readString(Path.of("infra", "shop-realm.json")));

    RealmFileTest() throws Exception {
    }

    private JsonNode client(String id) {
        for (JsonNode client : realm.get("clients")) {
            if (client.get("clientId").asString().equals(id)) {
                return client;
            }
        }
        throw new AssertionError("no client " + id);
    }

    @Test
    void theRealmHasTheThreeRoles() {
        var names = realm.get("roles").get("realm").valueStream().map(r -> r.get("name").asString()).toList();
        assertEquals(java.util.List.of("customer", "support", "admin"), names);
    }

    @Test
    void thePublicClientUsesPkceAndNoPasswordGrant() {
        JsonNode web = client("shop-web");
        assertTrue(web.get("publicClient").asBoolean());
        assertEquals("S256", web.get("attributes").get("pkce.code.challenge.method").asString());
        assertFalse(web.get("directAccessGrantsEnabled").asBoolean());
        assertFalse(web.get("implicitFlowEnabled").asBoolean());
        assertFalse(web.get("serviceAccountsEnabled").asBoolean());
    }

    @Test
    void redirectUrisAreExactNotWildcards() {
        for (JsonNode uri : client("shop-web").get("redirectUris")) {
            assertFalse(uri.asString().contains("*"));
        }
    }

    @Test
    void theConfidentialClientIsAServiceAccountOnly() {
        JsonNode batch = client("shop-batch");
        assertFalse(batch.get("publicClient").asBoolean());
        assertTrue(batch.get("serviceAccountsEnabled").asBoolean());
        assertFalse(batch.get("standardFlowEnabled").asBoolean());
    }

    @Test
    void bothClientsAddTheShopApiAudience() {
        for (String id : new String[] {"shop-web", "shop-batch"}) {
            JsonNode mapper = client(id).get("protocolMappers").get(0);
            assertEquals("oidc-audience-mapper", mapper.get("protocolMapper").asString());
            assertEquals("shop-api", mapper.get("config").get("included.custom.audience").asString());
        }
    }

    @Test
    void accessTokensAreShortAndLoginsAreRateLimited() {
        assertTrue(realm.get("accessTokenLifespan").asInt() <= 300);
        assertTrue(realm.get("bruteForceProtected").asBoolean());
        assertFalse(realm.get("registrationAllowed").asBoolean());
    }

    @Test
    void passwordsInTheRealmAreDemoValuesAndMarkedSo() {
        for (JsonNode user : realm.get("users")) {
            if (user.has("credentials")) {
                assertEquals("demo-password", user.get("credentials").get(0).get("value").asString());
            }
        }
    }
}
