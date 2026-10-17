package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * What too many APIs do: split the token, decode the claims, check that it has not expired, and trust the subject. It never looks at
 * the signature, the algorithm, the key, the issuer or the audience. Kept here so the tests can show what each missing check lets in.
 */
final class NaiveReader {

    private final SimClock clock;

    NaiveReader(SimClock clock) {
        this.clock = clock;
    }

    /** The subject the token claims, or empty if it looks expired or broken. */
    java.util.Optional<String> subject(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            Map<String, Object> claims = Json.parseObject(new String(Base64Url.decode(parts[1]), StandardCharsets.UTF_8));
            if (claims.get("exp") instanceof Long exp && clock.now() >= exp) {
                return java.util.Optional.empty();
            }
            return claims.get("sub") instanceof String s ? java.util.Optional.of(s) : java.util.Optional.empty();
        } catch (InvalidTokenException | ArrayIndexOutOfBoundsException e) {
            return java.util.Optional.empty();
        }
    }
}
