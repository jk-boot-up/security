package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Validates a JWT the way a resource server must: the algorithm is chosen by the verifier (RS256 only), the key by the key id from the
 * issuer's published key set, and the signature is checked before any claim is trusted. Then issuer, audience and expiry.
 */
final class Jwt {

    /** The outcome: valid or not, why, who (only when the signature was good) and the claims. */
    record Result(boolean valid, String reason, String user, Map<String, Object> claims) {

        static Result invalid(String reason, String user, Map<String, Object> claims) {
            return new Result(false, reason, user, claims);
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private Jwt() {
    }

    static Result verify(String token, Map<String, PublicKey> keys, String issuer, String audience, Instant now) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            return Result.invalid("malformed token", "-", Map.of());
        }
        Map<String, Object> header;
        Map<String, Object> claims;
        byte[] signature;
        try {
            header = JSON.readValue(Base64.getUrlDecoder().decode(parts[0]), MAP);
            claims = JSON.readValue(Base64.getUrlDecoder().decode(parts[1]), MAP);
            signature = Base64.getUrlDecoder().decode(parts[2]);
        } catch (Exception e) {
            return Result.invalid("malformed token", "-", Map.of());
        }
        if (!"RS256".equals(header.get("alg"))) {
            return Result.invalid("unsupported algorithm", "-", Map.of());
        }
        PublicKey key = keys.get(String.valueOf(header.get("kid")));
        if (key == null) {
            return Result.invalid("unknown signing key", "-", Map.of());
        }
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(key);
            s.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!s.verify(signature)) {
                return Result.invalid("bad signature", "-", Map.of());
            }
        } catch (GeneralSecurityException e) {
            return Result.invalid("bad signature", "-", Map.of());
        }
        // from here on the claims are the issuer's own words
        String user = String.valueOf(claims.getOrDefault("preferred_username", "-"));
        if (!issuer.equals(claims.get("iss"))) {
            return Result.invalid("wrong issuer", user, claims);
        }
        if (!audiences(claims.get("aud")).contains(audience)) {
            return Result.invalid("wrong audience", user, claims);
        }
        if (!(claims.get("exp") instanceof Number exp) || !now.isBefore(Instant.ofEpochSecond(exp.longValue()))) {
            return Result.invalid("expired", user, claims);
        }
        return new Result(true, "valid", user, claims);
    }

    private static List<?> audiences(Object aud) {
        if (aud instanceof List<?> list) {
            return list;
        }
        return aud == null ? List.of() : List.of(aud);
    }
}
