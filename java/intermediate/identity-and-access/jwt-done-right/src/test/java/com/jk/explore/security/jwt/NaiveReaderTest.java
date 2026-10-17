package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Each check the naive reader skips, shown as a token it believes and the validator refuses. */
class NaiveReaderTest {

    private static final long START = JwtDemo.START;

    private final SimClock clock = new SimClock(START + 10);
    private final NaiveReader naive = new NaiveReader(clock);
    private final Validator validator = new Validator(Policy.bank(), JwtDemo.bankKeys(), clock);

    private void naiveBelievesButValidatorRefuses(String token, String reason) {
        assertTrue(naive.subject(token).isPresent(), "the naive reader trusts it");
        assertEquals(reason, assertThrows(InvalidTokenException.class, () -> validator.validate(token)).getMessage());
    }

    private static String encode(Map<String, Object> json) {
        return Base64Url.encode(Json.write(json).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void editedClaims() {
        String token = Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + encode(Issuer.claims("mallory", "accounts:read", START, 300)) + "." + parts[2];
        assertEquals("mallory", naive.subject(forged).orElseThrow());
        naiveBelievesButValidatorRefuses(forged, "signature does not match");
    }

    @Test
    void noSignatureAtAll() {
        Map<String, Object> header = new LinkedHashMap<>(Issuer.header(JwtDemo.OLD_KID));
        header.put("alg", "none");
        naiveBelievesButValidatorRefuses(encode(header) + "." + encode(Issuer.claims("ana", "accounts:read", START, 300)) + ".",
                "algorithm none is not EdDSA");
    }

    @Test
    void aKeyTheBankNeverPublished() {
        naiveBelievesButValidatorRefuses(Issuer.issue(JwtDemo.OLD_KID, JwtDemo.INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300),
                "signature does not match");
    }

    @Test
    void anotherIssuerOrAudience() {
        Map<String, Object> claims = Issuer.claims("ana", "accounts:read", START, 300);
        claims.put("iss", "https://auth.partner.example");
        naiveBelievesButValidatorRefuses(Issuer.issue(JwtDemo.OLD_KEY.getPrivate(), Issuer.header(JwtDemo.OLD_KID), claims), "another issuer");
        claims = Issuer.claims("ana", "accounts:read", START, 300);
        claims.put("aud", "loyalty-api");
        naiveBelievesButValidatorRefuses(Issuer.issue(JwtDemo.OLD_KEY.getPrivate(), Issuer.header(JwtDemo.OLD_KID), claims), "another audience");
    }

    @Test
    void theWrongScope() {
        naiveBelievesButValidatorRefuses(Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "cards:read", START, 300),
                "missing scope accounts:read");
    }

    @Test
    void theNaiveReaderDoesCheckExpiry() {
        String token = Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        clock.advance(400);
        assertTrue(naive.subject(token).isEmpty());
    }
}
