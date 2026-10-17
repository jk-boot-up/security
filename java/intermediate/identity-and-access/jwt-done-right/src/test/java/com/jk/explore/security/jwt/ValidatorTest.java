package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    private static final long START = JwtDemo.START;

    private static Validator validator(SimClock clock) {
        return new Validator(Policy.bank(), JwtDemo.bankKeys(), clock);
    }

    private static String sign(Map<String, Object> header, Map<String, Object> claims) {
        return Issuer.issue(JwtDemo.OLD_KEY.getPrivate(), header, claims);
    }

    private static Map<String, Object> claims() {
        return Issuer.claims("ana", "accounts:read", START, 300);
    }

    private static Map<String, Object> header() {
        return new LinkedHashMap<>(Issuer.header(JwtDemo.OLD_KID));
    }

    private static String reason(String token, SimClock clock) {
        return assertThrows(InvalidTokenException.class, () -> validator(clock).validate(token)).getMessage();
    }

    @Test
    void aGoodTokenIsAccepted() throws Exception {
        AccessToken token = validator(new SimClock(START)).validate(Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(),
                "ana", "accounts:read accounts:write", START, 300));
        assertEquals("ana", token.subject());
        assertEquals(START + 300, token.expiresAt());
    }

    @Test
    void theAudienceMayBeAList() throws Exception {
        Map<String, Object> claims = claims();
        claims.put("aud", List.of("loyalty-api", "accounts-api"));
        assertEquals("ana", validator(new SimClock(START)).validate(sign(header(), claims)).subject());
    }

    @Test
    void theAlgorithmComesFromThePolicyNotTheHeader() {
        for (String alg : List.of("none", "HS256", "RS256", "ES256", "eddsa")) {
            Map<String, Object> header = header();
            header.put("alg", alg);
            assertEquals("algorithm " + alg + " is not EdDSA", reason(sign(header, claims()), new SimClock(START)));
        }
        Map<String, Object> header = header();
        header.remove("alg");
        assertEquals("algorithm null is not EdDSA", reason(sign(header, claims()), new SimClock(START)));
    }

    @Test
    void anUnsignedTokenIsRefused() {
        Map<String, Object> header = header();
        header.put("alg", "none");
        String token = Base64Url.encode(Json.write(header).getBytes(StandardCharsets.UTF_8)) + "."
                + Base64Url.encode(Json.write(claims()).getBytes(StandardCharsets.UTF_8)) + ".";
        assertEquals("algorithm none is not EdDSA", reason(token, new SimClock(START)));
    }

    @Test
    void theTypeMustBeAnAccessToken() {
        Map<String, Object> header = header();
        header.put("typ", "JWT");
        assertEquals("type JWT is not at+jwt", reason(sign(header, claims()), new SimClock(START)));
        header.remove("typ");
        assertEquals("type null is not at+jwt", reason(sign(header, claims()), new SimClock(START)));
    }

    @Test
    void theTypeIsComparedWithoutRegardToCaseWhateverTheLocale() throws Exception {
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.of("tr"));
            Map<String, Object> header = header();
            header.put("typ", "AT+JWT");
            assertEquals("ana", validator(new SimClock(START)).validate(sign(header, claims())).subject());
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void aCriticalHeaderIsRefused() {
        Map<String, Object> header = header();
        header.put("crit", List.of("exp"));
        assertEquals("a critical header the API does not understand", reason(sign(header, claims()), new SimClock(START)));
    }

    @Test
    void expiryAllowsOnlyTheSkew() throws Exception {
        String token = sign(header(), claims());
        validator(new SimClock(START + 329)).validate(token);
        assertEquals("expired", reason(token, new SimClock(START + 330)));
    }

    @Test
    void notBeforeAllowsOnlyTheSkew() throws Exception {
        Map<String, Object> claims = claims();
        claims.put("nbf", START + 100);
        String token = sign(header(), claims);
        assertEquals("not valid yet", reason(token, new SimClock(START + 69)));
        validator(new SimClock(START + 70)).validate(token);
    }

    @Test
    void issuedInTheFutureIsRefused() {
        Map<String, Object> claims = Issuer.claims("ana", "accounts:read", START + 600, 300);
        claims.remove("nbf");
        assertEquals("issued in the future", reason(sign(header(), claims), new SimClock(START)));
    }

    @Test
    void requiredClaimsMustBePresentAndTyped() {
        for (String name : List.of("exp", "iat")) {
            Map<String, Object> claims = claims();
            claims.remove(name);
            assertEquals("no " + name + " claim", reason(sign(header(), claims), new SimClock(START)));
            claims.put(name, "1800000300");
            assertEquals("no " + name + " claim", reason(sign(header(), claims), new SimClock(START)));
        }
        Map<String, Object> claims = claims();
        claims.remove("sub");
        assertEquals("no subject", reason(sign(header(), claims), new SimClock(START)));
    }

    @Test
    void issuerAudienceScopeAndLifetimeAreChecked() {
        Map<String, Object> claims = claims();
        claims.put("iss", "https://auth.bank.example.evil");
        assertEquals("another issuer", reason(sign(header(), claims), new SimClock(START)));
        claims = claims();
        claims.put("aud", List.of("loyalty-api"));
        assertEquals("another audience", reason(sign(header(), claims), new SimClock(START)));
        claims = claims();
        claims.put("scope", "accounts:readonly");
        assertEquals("missing scope accounts:read", reason(sign(header(), claims), new SimClock(START)));
        assertEquals("lifetime longer than the policy allows",
                reason(sign(header(), Issuer.claims("ana", "accounts:read", START, 901)), new SimClock(START)));
    }

    @Test
    void malformedTokensAreRefusedBeforeAnythingIsTrusted() {
        assertEquals("not three parts", reason("a.b", new SimClock(START)));
        assertEquals("not three parts", reason("a.b.c.d", new SimClock(START)));
        assertEquals("a part is not base64url", reason("e30=.e30.AA", new SimClock(START)));
        assertEquals("a part is not base64url", reason("e3+0.e30.AA", new SimClock(START)));
        String duplicate = Base64Url.encode("{\"alg\":\"EdDSA\",\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + ".e30.AA";
        assertEquals("duplicate member \"alg\"", reason(duplicate, new SimClock(START)));
    }
}
