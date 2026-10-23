package com.jk.explore.security.zerotrust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtTest {

    private static final String ISSUER = "https://sso.bank.example/realms/bank";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static KeyPair key;
    private static Map<String, PublicKey> keys;
    private static Map<String, Object> header;

    @BeforeAll
    static void makeKey() throws Exception {
        key = Forge.newKey();
        keys = Map.of("k1", key.getPublic());
        header = Map.of("alg", "RS256", "kid", "k1");
    }

    private static Map<String, Object> claims(String audience, long exp) {
        return Forge.claims(ISSUER, audience, "alice", List.of("teller"), true, exp);
    }

    private static Jwt.Result check(String token) {
        return Jwt.verify(token, keys, ISSUER, "ledger-api", NOW);
    }

    @Test
    void aGoodTokenIsValidAndNamesTheUser() throws Exception {
        Jwt.Result r = check(Forge.token(header, claims("ledger-api", NOW.getEpochSecond() + 60), key));
        assertTrue(r.valid());
        assertEquals("alice", r.user());
    }

    @Test
    void anExpiredTokenIsRefusedAtTheSecondItExpires() throws Exception {
        assertFalse(check(Forge.token(header, claims("ledger-api", NOW.getEpochSecond()), key)).valid());
        assertEquals("expired", check(Forge.token(header, claims("ledger-api", NOW.getEpochSecond() - 1), key)).reason());
    }

    @Test
    void aTokenForAnotherAudienceIsRefused() throws Exception {
        assertEquals("wrong audience", check(Forge.token(header, claims("reports-api", NOW.getEpochSecond() + 60), key)).reason());
    }

    @Test
    void anAudienceListIsAccepted() throws Exception {
        Map<String, Object> c = new java.util.HashMap<>(claims("x", NOW.getEpochSecond() + 60));
        c.put("aud", List.of("account", "ledger-api"));
        assertTrue(check(Forge.token(header, c, key)).valid());
    }

    @Test
    void aTokenFromAnotherIssuerIsRefused() throws Exception {
        Map<String, Object> c = new java.util.HashMap<>(claims("ledger-api", NOW.getEpochSecond() + 60));
        c.put("iss", "https://evil.example");
        assertEquals("wrong issuer", check(Forge.token(header, c, key)).reason());
    }

    @Test
    void editedClaimsBreakTheSignature() throws Exception {
        String good = Forge.token(header, claims("ledger-api", NOW.getEpochSecond() + 60), key);
        String edited = Forge.withEditedClaims(good, Map.of("realm_access", Map.of("roles", List.of("admin"))));
        assertEquals("bad signature", check(edited).reason());
        assertEquals("-", check(edited).user());
    }

    @Test
    void aFlippedSignatureCharacterIsRefused() throws Exception {
        String good = Forge.token(header, claims("ledger-api", NOW.getEpochSecond() + 60), key);
        assertEquals("bad signature", check(Forge.withFlippedSignature(good)).reason());
    }

    @Test
    void aTokenSignedByAnotherKeyIsRefused() throws Exception {
        assertEquals("bad signature", check(Forge.token(header, claims("ledger-api", NOW.getEpochSecond() + 60), Forge.newKey())).reason());
    }

    @Test
    void theAlgorithmNoneIsRefused() throws Exception {
        assertEquals("unsupported algorithm", check(Forge.token(Map.of("alg", "none"), claims("ledger-api", NOW.getEpochSecond() + 60), null)).reason());
        assertEquals("unsupported algorithm", check(Forge.token(Map.of("alg", "HS256", "kid", "k1"), claims("ledger-api", NOW.getEpochSecond() + 60), key)).reason());
    }

    @Test
    void anUnknownKeyIdAndGarbageAreRefused() throws Exception {
        assertEquals("unknown signing key", check(Forge.token(Map.of("alg", "RS256", "kid", "other"), claims("ledger-api", NOW.getEpochSecond() + 60), key)).reason());
        assertEquals("malformed token", check("not-a-token").reason());
        assertEquals("malformed token", check("a.b.c").reason());
    }
}
