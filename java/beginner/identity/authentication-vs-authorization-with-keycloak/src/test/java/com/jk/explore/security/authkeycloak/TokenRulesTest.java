package com.jk.explore.security.authkeycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;

/** The order API with tokens from the stand-in issuer: the rules a resource server must apply. */
class TokenRulesTest {

    private SimClock clock;
    private Issuer issuer;
    private Revocations revocations;
    private Running api;

    @BeforeEach
    void start() {
        clock = new SimClock(Instant.parse("2026-01-01T09:00:00Z"));
        issuer = new Issuer(Issuer.REALM_ISSUER, clock);
        revocations = new Revocations();
        api = Running.start(KeycloakDemo.decoder(issuer, clock, List.of(revocations)));
    }

    @AfterEach
    void stop() {
        api.close();
    }

    private String token(String user, String role) {
        return issuer.mint(user, List.of(role), 300, Issuer.AUDIENCE);
    }

    @Test
    void noTokenIs401NotThe403() throws Exception {
        assertEquals(401, api.status("/orders/A-1", null));
        assertEquals(401, api.status("/admin/report", null));
    }

    @Test
    void aValidTokenWithoutTheRoleIs403() throws Exception {
        assertEquals(403, api.status("/admin/report", token("ana", "customer")));
        assertEquals(403, api.status("/admin/report", token("sam", "support")));
        assertEquals(200, api.status("/admin/report", token("root", "admin")));
    }

    @Test
    void shoppersSeeOnlyTheirOwnOrders() throws Exception {
        String ana = token("ana", "customer");
        assertEquals(200, api.status("/orders/A-1", ana));
        assertEquals(403, api.status("/orders/B-2", ana));
        assertEquals(404, api.status("/orders/Z-9", ana));
        assertEquals(200, api.status("/orders/B-2", token("sam", "support")));
    }

    @Test
    void aTokenWorksUpToItsExpiryInstantAndNotAfter() throws Exception {
        String token = issuer.mint("ana", List.of("customer"), 60, Issuer.AUDIENCE);
        clock.advanceSeconds(60);
        assertEquals(200, api.status("/me", token), "at the expiry instant itself the token is still accepted");
        clock.advanceSeconds(1);
        assertEquals(401, api.status("/me", token));
    }

    @Test
    void aForgedAndAnUnsignedTokenAreRefused() throws Exception {
        assertEquals(401, api.status("/admin/report", issuer.forge("root", List.of("admin"))));
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"" + Issuer.REALM_ISSUER + "\",\"aud\":\"shop-api\",\"exp\":4102444800,\"realm_access\":{\"roles\":[\"admin\"]}}").getBytes());
        assertEquals(401, api.status("/admin/report", header + "." + payload + "."));
    }

    @Test
    void aChangedPayloadBreaksTheSignature() throws Exception {
        String[] parts = token("ana", "customer").split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1])).replace("customer", "admin   ");
        String changed = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes()) + "." + parts[2];
        assertEquals(401, api.status("/admin/report", changed));
    }

    @Test
    void issuerAndAudienceMustMatch() throws Exception {
        assertEquals(401, api.status("/me", issuer.mintWithIssuer("ana", List.of("customer"), 300, Issuer.AUDIENCE, "http://other.example/realms/x")));
        assertEquals(401, api.status("/me", issuer.mint("ana", List.of("customer"), 300, "another-api")));
    }

    @Test
    void aTokenWithoutRolesGetsNoAuthorities() throws Exception {
        String token = issuer.mint("nobody", List.of(), 300, Issuer.AUDIENCE);
        assertEquals("nobody []", api.get("/me", token).body());
        assertEquals(403, api.status("/admin/report", token));
    }

    @Test
    void roleNamesAreMatchedWhateverTheCase() throws Exception {
        assertEquals(200, api.status("/admin/report", token("root", "Admin")));
    }

    @Test
    void aRevokedTokenIsRefusedAndEveryRequestCostsALookup() throws Exception {
        String token = token("root", "admin");
        assertEquals(200, api.status("/admin/report", token));
        revocations.revoke(issuer.lastTokenId());
        assertEquals(401, api.status("/admin/report", token));
        assertEquals(2, revocations.lookups());
    }

    @Test
    void aRoleChangeDoesNotReachATokenAlreadyIssued() throws Exception {
        String admin = token("root", "admin");
        String demoted = token("root", "customer");
        assertEquals(200, api.status("/admin/report", admin));
        assertEquals(403, api.status("/admin/report", demoted));
        clock.advanceSeconds(301);
        assertEquals(401, api.status("/admin/report", admin));
    }

    @Test
    void theDecoderRejectsBadTokensWithAnException() {
        var decoder = KeycloakDemo.decoder(issuer, clock, List.of());
        assertThrows(BadJwtException.class, () -> decoder.decode("not.a.token"));
        assertTrue(decoder.decode(token("ana", "customer")).getAudience().contains(Issuer.AUDIENCE));
    }
}
