package com.jk.explore.security.authkeycloak;

import java.util.HashSet;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** A list of token ids that were revoked. Checking it on every request is the price of ending a token early. */
final class Revocations implements OAuth2TokenValidator<Jwt> {

    private final Set<String> revoked = new HashSet<>();
    private int lookups;

    void revoke(String tokenId) {
        revoked.add(tokenId);
    }

    int lookups() {
        return lookups;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        lookups++;
        if (revoked.contains(jwt.getId())) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "the token was revoked", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
