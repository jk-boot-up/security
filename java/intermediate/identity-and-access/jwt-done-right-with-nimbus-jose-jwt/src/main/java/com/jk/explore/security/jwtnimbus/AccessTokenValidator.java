package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URL;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * What the accounts API does with a bearer token. The algorithm and the type are settings of the API, the key comes from the bank's published set at the one URL
 * the API was configured with, and the claims are checked against this API and the clock. Nimbus does the parsing, the signature and most of the claims; the scope
 * and the lifetime policy are two checks of our own.
 */
final class AccessTokenValidator {

    record Accepted(String subject, String scope) {
    }

    static final int CLOCK_SKEW_SECONDS = 30;
    static final Duration MAX_LIFETIME = Duration.ofMinutes(15);

    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    private final String requiredScope;

    /**
     * @param jwks the one URL the API fetches keys from, however a token is written
     * @param cacheMillis how long a fetched key set is kept before the API asks again; the library can also refresh ahead of the expiry, which the lab switches off so that the expiry is visible
     * @param minRefetchMillis the least time between two fetches that an unknown key id may cause
     */
    AccessTokenValidator(URL jwks, long cacheMillis, long minRefetchMillis, Clock clock, String audience, String requiredScope) {
        this.requiredScope = requiredScope;
        JWKSource<SecurityContext> keys = JWKSourceBuilder.<SecurityContext>create(jwks).cache(cacheMillis, 1_000L).refreshAheadCache(false).rateLimited(minRefetchMillis).retrying(true).build();
        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(BankIssuer.ACCESS_TOKEN));
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, keys));
        DefaultJWTClaimsVerifier<SecurityContext> claims = new DefaultJWTClaimsVerifier<>(Set.of(audience),
                new JWTClaimsSet.Builder().issuer(BankIssuer.ISSUER).build(), Set.of("sub", "iss", "aud", "exp", "iat", "nbf", "scope"), Set.of()) {
            @Override
            protected Date currentTime() {
                return Date.from(clock.instant());
            }
        };
        claims.setMaxClockSkew(CLOCK_SKEW_SECONDS);
        processor.setJWTClaimsSetVerifier(claims);
    }

    /** Returns who the token is for, or says why not. */
    Accepted validate(String token) throws Refused {
        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (BadJOSEException e) {
            throw new Refused(e.getMessage(), e);
        } catch (ParseException e) {
            throw new Refused("not a token: " + e.getMessage(), e);
        } catch (JOSEException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new Refused("the key set could not be asked again so soon (" + cause.getClass().getSimpleName() + ")", e);
        }
        String scope = String.valueOf(claims.getClaim("scope"));
        if (!List.of(scope.split(" ")).contains(requiredScope)) {
            throw new Refused("missing scope " + requiredScope);
        }
        Duration lifetime = Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant());
        if (lifetime.compareTo(MAX_LIFETIME) > 0) {
            throw new Refused("lifetime longer than the policy allows");
        }
        return new Accepted(claims.getSubject(), scope);
    }
}
