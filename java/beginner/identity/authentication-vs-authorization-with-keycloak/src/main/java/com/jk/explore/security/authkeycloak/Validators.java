package com.jk.explore.security.authkeycloak;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/** The checks a resource server makes after the signature: expiry, issuer, audience, and optionally a revocation list. */
final class Validators {

    private Validators() {
    }

    /** Accepts only tokens meant for this API. Keycloak does not add the audience unless a mapper is configured. */
    static OAuth2TokenValidator<Jwt> audience(String expected) {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(expected)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "the token is not meant for " + expected, null));
    }

    static OAuth2TokenValidator<Jwt> all(String issuer, String audience, Clock clock, List<OAuth2TokenValidator<Jwt>> extra) {
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        List<OAuth2TokenValidator<Jwt>> list = new ArrayList<>();
        list.add(timestamps);
        list.add(new JwtIssuerValidator(issuer));
        list.add(audience(audience));
        list.addAll(extra);
        return new DelegatingOAuth2TokenValidator<>(list);
    }
}
