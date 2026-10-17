package com.jk.explore.security.authkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** The shop's order API against a real Keycloak in Docker. Run with ./gradlew integrationTest. */
@Tag("docker")
class KeycloakIntegrationTest {

    private static Keycloak keycloak;
    private static Running api;

    private static JwtDecoder decoder(String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keycloak.issuer() + "/protocol/openid-connect/certs").build();
        decoder.setJwtValidator(Validators.all(keycloak.issuer(), audience, Clock.systemUTC(), List.of()));
        return decoder;
    }

    @BeforeAll
    static void start() throws Exception {
        keycloak = Keycloak.start();
        api = Running.start(decoder(Issuer.AUDIENCE));
    }

    @AfterAll
    static void stop() {
        if (api != null) {
            api.close();
        }
        if (keycloak != null) {
            keycloak.close();
        }
    }

    private static JWTClaimsSet claims(String token) throws Exception {
        return SignedJWT.parse(token).getJWTClaimsSet();
    }

    @Test
    void aShopperLogsInWithPkceAndGetsTheShopperRole() throws Exception {
        String token = keycloak.login("ana", "demo-password");
        JWTClaimsSet claims = claims(token);
        assertEquals(keycloak.issuer(), claims.getIssuer());
        assertTrue(claims.getAudience().contains("shop-api"));
        assertEquals("shop-web", claims.getStringClaim("azp"));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) ((Map<String, Object>) claims.getClaim("realm_access")).get("roles");
        assertTrue(roles.contains("customer"));
        assertEquals(200, api.status("/orders/A-1", token));
        assertEquals(403, api.status("/orders/B-2", token));
        assertEquals(403, api.status("/admin/report", token));
        assertTrue(api.get("/me", token).body().contains("ROLE_CUSTOMER"));
    }

    @Test
    void supportAndAdminRolesOpenMore() throws Exception {
        String sam = keycloak.login("sam", "demo-password");
        String root = keycloak.login("root", "demo-password");
        assertEquals(200, api.status("/orders/B-2", sam));
        assertEquals(403, api.status("/admin/report", sam));
        assertEquals(200, api.status("/admin/report", root));
    }

    @Test
    void theNightlyJobUsesItsSecretAndHasTheSupportRole() throws Exception {
        String token = keycloak.clientCredentials("shop-batch", "demo-batch-secret");
        assertEquals(200, api.status("/orders/B-2", token));
        assertEquals(403, api.status("/admin/report", token));
    }

    @Test
    void noTokenAndATamperedTokenAreRefused() throws Exception {
        String token = keycloak.login("ana", "demo-password");
        assertEquals(401, api.status("/me", null));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + parts[2].substring(0, parts[2].length() - 4) + "AAAA";
        assertEquals(401, api.status("/me", tampered));
    }

    @Test
    void aWrongPasswordIsRefusedByTheLoginPage() throws Exception {
        assertTrue(keycloak.loginRefused("ana", "wrong-password"));
    }

    @Test
    void aTokenMeantForAnotherApiIsRefused() throws Exception {
        try (Running other = Running.start(decoder("another-api"))) {
            assertEquals(401, other.status("/me", keycloak.login("ana", "demo-password")));
        }
    }

    @Test
    void aRemovedRoleWorksUntilTheTokenExpires() throws Exception {
        String old = keycloak.login("temp-admin", "demo-password");
        assertEquals(200, api.status("/admin/report", old));
        keycloak.removeRealmRole("temp-admin", "admin");
        assertEquals(200, api.status("/admin/report", old), "the token still carries the role");
        String fresh = keycloak.login("temp-admin", "demo-password");
        assertEquals(403, api.status("/admin/report", fresh), "a new token does not");
        long lifetime = claims(old).getExpirationTime().getTime() / 1000 - claims(old).getIssueTime().getTime() / 1000;
        assertEquals(15, lifetime);
        Thread.sleep((lifetime + 2) * 1000);
        assertEquals(401, api.status("/admin/report", old), "after expiry the old token is refused");
    }
}
