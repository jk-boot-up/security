package com.jk.explore.security.oidcspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/** Spring Security's ID token checks, against tokens signed by a local provider with claims that Keycloak would never issue. No Docker is needed. */
class ProviderChecksTest {

    private LocalProvider provider;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        provider = new LocalProvider(Clock.systemUTC());
        ClientRegistration registration = ClientRegistration.withRegistrationId("lab").clientId(LocalProvider.CLIENT).clientSecret("unused")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://127.0.0.1/cb").authorizationUri("https://bank.example/auth")
                .tokenUri("https://bank.example/token").jwkSetUri(provider.jwksUri()).issuerUri(LocalProvider.ISSUER).scope("openid").build();
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(r -> {
            OidcIdTokenValidator oidc = new OidcIdTokenValidator(r);
            oidc.setClockSkew(Duration.ofSeconds(30));
            return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ofSeconds(30)), oidc);
        });
        decoder = factory.createDecoder(registration);
    }

    @AfterEach
    void tearDown() {
        provider.close();
    }

    private String refused(String token) {
        return assertThrows(JwtException.class, () -> decoder.decode(token)).getMessage();
    }

    @Test
    void theProvidersOwnTokenIsAccepted() throws Exception {
        Jwt jwt = decoder.decode(provider.sign(provider.claims().build()));
        assertEquals("ana-0001", jwt.getSubject());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRefused() throws Exception {
        assertTrue(refused(provider.signWithAnotherKey(provider.claims().build())).contains("Invalid signature"));
    }

    @Test
    void theIssuerMustBeExactlyTheConfiguredOne() throws Exception {
        assertTrue(refused(provider.sign(provider.claims().issuer("https://bank.example/").build())).contains("iss"));
        assertTrue(refused(provider.sign(provider.claims().issuer("https://wallet.example").build())).contains("iss"));
        assertTrue(refused(provider.sign(provider.claims().issuer("https://bank.example.evil.test").build())).contains("iss"));
    }

    @Test
    void theAudienceMustIncludeThisClient() throws Exception {
        assertTrue(refused(provider.sign(provider.claims().audience("budget-app").build())).contains("aud"));
    }

    @Test
    void severalAudiencesNeedAnAzpNamingThisClient() throws Exception {
        JWTClaimsSet.Builder two = provider.claims().audience(List.of("store-web", "budget-app"));
        assertTrue(refused(provider.sign(two.build())).contains("azp"));
        assertTrue(refused(provider.sign(two.claim("azp", "budget-app").build())).contains("azp"));
        assertEquals("ana-0001", decoder.decode(provider.sign(two.claim("azp", "store-web").build())).getSubject());
    }

    @Test
    void expiryAllowsThirtySecondsOfSkew() throws Exception {
        assertEquals("ana-0001", decoder.decode(provider.sign(provider.expiredSecondsAgo(29).build())).getSubject());
        assertTrue(refused(provider.sign(provider.expiredSecondsAgo(31).build())).contains("Jwt expired"));
    }

    @Test
    void aTokenFromTheFutureIsRefused() throws Exception {
        JWTClaimsSet future = provider.claims().issueTime(java.util.Date.from(java.time.Instant.now().plusSeconds(120))).expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(500))).build();
        assertTrue(refused(provider.sign(future)).contains("iat"));
    }

    @Test
    void aTokenWithNoSubjectIsRefused() throws Exception {
        assertTrue(refused(provider.sign(provider.claims().subject(null).build())).contains("sub"));
    }

    @Test
    void anUnsignedTokenIsRefused() {
        assertTrue(refused(provider.unsigned(provider.claims().build())).contains("none"));
    }

    @Test
    void discoveryRefusesADocumentThatNamesAnotherIssuer() {
        provider.serveDiscovery("https://wallet.example", provider.jwksUri());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ClientRegistrations.fromIssuerLocation(provider.issuerAddress()));
        assertTrue(e.getMessage().startsWith("The Issuer \"https://wallet.example\""));
    }

    @Test
    void discoveryDoesNotCheckWhereTheKeysAre() {
        provider.serveDiscovery(provider.issuerAddress(), "https://keys.example/jwks.json");
        assertEquals("https://keys.example/jwks.json", ClientRegistrations.fromIssuerLocation(provider.issuerAddress()).clientId("x").build().getProviderDetails().getJwkSetUri());
    }

    @Test
    void discoveryAcceptsADocumentThatNamesTheRequestedIssuer() {
        provider.serveDiscovery(provider.issuerAddress(), provider.jwksUri());
        ClientRegistration r = ClientRegistrations.fromIssuerLocation(provider.issuerAddress()).clientId("x").build();
        assertEquals(provider.issuerAddress(), r.getProviderDetails().getIssuerUri());
        assertEquals(provider.jwksUri(), r.getProviderDetails().getJwkSetUri());
    }
}
