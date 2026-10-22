package com.jk.explore.security.oidcspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static Map<String, Object> realm(String name) throws Exception {
        return JsonMapper.builder().build().readValue(Files.readString(Path.of("infra", name + "-realm.json")), new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> storeClient(String realm) throws Exception {
        return ((List<Map<String, Object>>) realm(realm).get("clients")).stream().filter(c -> "store-web".equals(c.get("clientId"))).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> user(String realm) throws Exception {
        return ((List<Map<String, Object>>) realm(realm).get("users")).get(0);
    }

    @Test
    void bothRealmsHaveOneConfidentialStoreClientWithTheSameSecret() throws Exception {
        for (String realm : List.of("bank", "wallet")) {
            assertEquals(false, storeClient(realm).get("publicClient"), realm);
            assertEquals("demo-store-secret", storeClient(realm).get("secret"), realm);
            assertEquals(false, storeClient(realm).get("implicitFlowEnabled"), realm);
            assertEquals(false, storeClient(realm).get("directAccessGrantsEnabled"), realm);
        }
    }

    @Test
    void redirectAddressesAreExactAndNameTheStoresPortAndTheProvider() throws Exception {
        for (String realm : List.of("bank", "wallet")) {
            @SuppressWarnings("unchecked")
            List<String> uris = (List<String>) storeClient(realm).get("redirectUris");
            assertEquals(List.of("http://127.0.0.1:${STORE_PORT}/login/oauth2/code/" + realm), uris);
        }
    }

    @Test
    void thePhoneAndLaptopLogoutIsConfiguredOnTheBankOnly() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) storeClient("bank").get("attributes");
        assertEquals("http://host.docker.internal:${STORE_PORT}/logout/connect/back-channel/bank", attributes.get("backchannel.logout.url"));
        assertEquals("true", attributes.get("backchannel.logout.session.required"));
        assertEquals("http://127.0.0.1:${STORE_PORT}/", attributes.get("post.logout.redirect.uris"));
        @SuppressWarnings("unchecked")
        Map<String, Object> wallet = (Map<String, Object>) storeClient("wallet").get("attributes");
        assertFalse(wallet.containsKey("backchannel.logout.url"));
    }

    @Test
    void pkceIsRequiredByBothClients() throws Exception {
        for (String realm : List.of("bank", "wallet")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) storeClient(realm).get("attributes");
            assertEquals("S256", attributes.get("pkce.code.challenge.method"), realm);
        }
    }

    @Test
    void anaIsVerifiedAtTheBankAndMalHasTheSameAddressUnverifiedAtTheWallet() throws Exception {
        assertEquals(AccountsKeys.ANA, user("bank").get("id"));
        assertEquals(true, user("bank").get("emailVerified"));
        assertEquals(AccountsKeys.MAL, user("wallet").get("id"));
        assertEquals(false, user("wallet").get("emailVerified"));
        assertEquals(user("bank").get("email"), user("wallet").get("email"));
        assertTrue(!user("bank").get("id").equals(user("wallet").get("id")));
    }
}
