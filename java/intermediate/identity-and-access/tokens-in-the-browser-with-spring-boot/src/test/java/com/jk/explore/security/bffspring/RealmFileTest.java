package com.jk.explore.security.bffspring;

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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> attributes(String id) {
        return (Map<String, Object>) client(id).get("attributes");
    }

    @Test
    void theTwoSinglePageClientsAreIdenticalExceptForDpop() {
        assertEquals(true, client("bank-spa").get("publicClient"));
        assertEquals(true, client("bank-spa-dpop").get("publicClient"));
        assertFalse(attributes("bank-spa").containsKey("dpop.bound.access.tokens"));
        assertEquals("true", attributes("bank-spa-dpop").get("dpop.bound.access.tokens"));
        assertEquals(client("bank-spa").get("redirectUris"), client("bank-spa-dpop").get("redirectUris"));
    }

    @Test
    void theBackendIsConfidentialAndItsRedirectNamesItsPort() {
        assertEquals(false, client("bank-bff").get("publicClient"));
        assertEquals("demo-bff-secret", client("bank-bff").get("secret"));
        assertEquals(List.of("http://127.0.0.1:${BFF_PORT}/login/oauth2/code/keycloak"), client("bank-bff").get("redirectUris"));
    }

    @Test
    void everyClientUsesPkceAndNeitherTheImplicitFlowNorThePasswordGrant() {
        for (String id : List.of("bank-spa", "bank-spa-dpop", "bank-bff")) {
            assertEquals("S256", attributes(id).get("pkce.code.challenge.method"), id);
            assertEquals(false, client(id).get("implicitFlowEnabled"), id);
            assertEquals(false, client(id).get("directAccessGrantsEnabled"), id);
        }
    }

    @Test
    void everyClientGetsTheAudienceOfTheAccountsApi() {
        for (String id : List.of("bank-spa", "bank-spa-dpop", "bank-bff")) {
            assertTrue(client(id).toString().contains("accounts-api"), id);
        }
    }

    @Test
    void accessTokensLastFiveMinutes() {
        assertEquals(300, realm.get("accessTokenLifespan"));
    }
}
