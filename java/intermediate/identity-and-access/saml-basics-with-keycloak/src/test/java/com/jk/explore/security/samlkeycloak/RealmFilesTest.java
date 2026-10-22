package com.jk.explore.security.samlkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** The settings that make the lesson true are in the realm files, so they are checked without starting Keycloak. */
class RealmFilesTest {

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> clients(String realm) throws Exception {
        Map<String, Object> r = JsonMapper.builder().build().readValue(Files.readString(Path.of("infra", realm + "-realm.json")), new TypeReference<Map<String, Object>>() {
        });
        return (List<Map<String, Object>>) r.get("clients");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> attributes(Map<String, Object> client) {
        return (Map<String, Object>) client.get("attributes");
    }

    @Test
    void bothRealmsHaveTheShopAsASamlClientThatSignsResponsesAndAssertions() throws Exception {
        for (String realm : List.of("bank", "wallet")) {
            Map<String, Object> shop = clients(realm).get(0);
            assertEquals("saml", shop.get("protocol"), realm);
            assertTrue(String.valueOf(shop.get("clientId")).endsWith("/saml2/service-provider-metadata/keycloak"), realm);
            assertEquals("true", attributes(shop).get("saml.server.signature"), realm);
            assertEquals("true", attributes(shop).get("saml.assertion.signature"), realm);
            assertEquals("RSA_SHA256", attributes(shop).get("saml.signature.algorithm"), realm);
            assertEquals("2", attributes(shop).get("saml.assertion.lifespan"), realm);
        }
    }

    @Test
    void theBankAlsoHasAnotherShopThatAnswersAtTheSameAddress() throws Exception {
        assertEquals(2, clients("bank").size());
        assertEquals(1, clients("wallet").size());
        assertEquals(clients("bank").get(0).get("redirectUris"), clients("bank").get(1).get("redirectUris"));
    }

    @Test
    void theRedirectAddressIsTheShopsExactAcsAddress() throws Exception {
        assertEquals(List.of("http://127.0.0.1:${SP_PORT}/login/saml2/sso/keycloak"), clients("bank").get(0).get("redirectUris"));
    }
}
