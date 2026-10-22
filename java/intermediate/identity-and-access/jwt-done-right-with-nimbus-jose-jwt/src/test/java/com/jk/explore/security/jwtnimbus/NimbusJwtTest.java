package com.jk.explore.security.jwtnimbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Real tokens, a real key-set server on a loopback port, and the real library. */
class NimbusJwtTest {

    private static final String SCOPE = "accounts:read";

    private SimClock clock;
    private BankIssuer bank;
    private JwksServer jwks;
    private AccessTokenValidator api;

    @BeforeEach
    void setUp() throws Exception {
        clock = new SimClock();
        bank = new BankIssuer(clock);
        bank.createKey("bank-2026-01");
        bank.signWith("bank-2026-01");
        jwks = new JwksServer();
        jwks.publish(bank.publicKeys());
        api = new AccessTokenValidator(jwks.url(), 300_000, 30_000, clock, BankIssuer.ACCOUNTS_API, SCOPE);
    }

    @AfterEach
    void tearDown() {
        jwks.close();
    }

    private String reason(String token) {
        return assertThrows(Refused.class, () -> api.validate(token)).getMessage();
    }

    private JWTClaimsSet claims(String sub) {
        return new JWTClaimsSet.Builder().issuer(BankIssuer.ISSUER).subject(sub).audience(BankIssuer.ACCOUNTS_API).claim("scope", SCOPE)
                .issueTime(Date.from(clock.instant())).notBeforeTime(Date.from(clock.instant())).expirationTime(Date.from(clock.instant().plus(Duration.ofMinutes(5)))).build();
    }

    @Test
    void aGoodTokenIsAcceptedForItsSubject() throws Exception {
        AccessTokenValidator.Accepted a = api.validate(bank.accessToken("ana", SCOPE));
        assertEquals("ana", a.subject());
        assertEquals(SCOPE, a.scope());
    }

    @Test
    void theSignatureIsSixtyFourBytes() throws Exception {
        assertEquals(64, SignedJWT.parse(bank.accessToken("ana", SCOPE)).getSignature().decode().length);
    }

    @Test
    void parsingTrustsTheClaimsAndVerifyingDoesNot() throws Exception {
        String[] parts = bank.accessToken("ana", SCOPE).split("\\.");
        String edited = new String(Base64.getUrlDecoder().decode(parts[1])).replace("\"sub\":\"ana\"", "\"sub\":\"mallory\"");
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(edited.getBytes()) + "." + parts[2];
        assertEquals("mallory", SignedJWT.parse(forged).getJWTClaimsSet().getSubject());
        assertTrue(reason(forged).contains("Invalid signature"));
    }

    @Test
    void anUnsignedTokenIsRefusedEvenWithTheRightType() {
        String plain = new PlainJWT(new PlainHeader.Builder().type(BankIssuer.ACCESS_TOKEN).build(), claims("mallory")).serialize();
        assertTrue(reason(plain).contains("Unsecured"));
    }

    @Test
    void anHmacTokenSignedWithThePublicKeyIsRefused() throws Exception {
        byte[] publicKey = bank.key("bank-2026-01").toPublicJWK().toECPublicKey().getEncoded();
        String hs256 = BankIssuer.sign(claims("mallory"), new JWSHeader.Builder(JWSAlgorithm.HS256).type(BankIssuer.ACCESS_TOKEN).keyID("bank-2026-01").build(), new MACSigner(publicKey));
        assertTrue(reason(hs256).contains("Another algorithm expected"));
    }

    @Test
    void aTokenOfAnotherTypeIsRefused() throws Exception {
        String token = BankIssuer.sign(claims("ana"), new JOSEObjectType("id+jwt"), "bank-2026-01", bank.key("bank-2026-01"));
        assertTrue(reason(token).contains("id+jwt not allowed"));
    }

    @Test
    void aTokenWithNoTypeIsRefused() throws Exception {
        String token = BankIssuer.sign(claims("ana"), new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("bank-2026-01").build(), new ECDSASigner(bank.key("bank-2026-01")));
        assertTrue(reason(token).contains("typ"));
    }

    @Test
    void clockSkewOfThirtySecondsIsAllowedAndNoMore() throws Exception {
        String token = bank.accessToken("ana", SCOPE);
        clock.advance(Duration.ofMinutes(5).plusSeconds(20));
        assertEquals("ana", api.validate(token).subject());
        clock.advance(Duration.ofSeconds(11));
        assertEquals("Expired JWT", reason(token));
    }

    @Test
    void aTokenThatStartsLaterIsRefused() throws Exception {
        String token = bank.accessToken("ana", SCOPE, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, Duration.ofMinutes(5), Duration.ofMinutes(2), "bank-2026-01");
        assertEquals("JWT before use time", reason(token));
        clock.advance(Duration.ofMinutes(2));
        assertEquals("ana", api.validate(token).subject());
    }

    @Test
    void anotherIssuerIsRefused() throws Exception {
        String token = bank.accessToken("ana", SCOPE, BankIssuer.ACCESS_TOKEN, "https://auth.other.example", BankIssuer.ACCOUNTS_API, Duration.ofMinutes(5), Duration.ZERO, "bank-2026-01");
        assertTrue(reason(token).contains("iss"));
    }

    @Test
    void anotherAudienceIsRefused() throws Exception {
        String token = bank.accessToken("ana", SCOPE, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.LOYALTY_API, Duration.ofMinutes(5), Duration.ZERO, "bank-2026-01");
        assertTrue(reason(token).contains("aud"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cards:read", "accounts:write", "accounts:read:all", ""})
    void aMissingScopeIsRefused(String scope) throws Exception {
        assertEquals("missing scope accounts:read", reason(bank.accessToken("ana", scope)));
    }

    @Test
    void severalScopesAreFineWhenTheRightOneIsAmongThem() throws Exception {
        assertEquals("ana", api.validate(bank.accessToken("ana", "cards:read accounts:read")).subject());
    }

    @Test
    void aTokenWithAMissingClaimIsRefused() throws Exception {
        JWTClaimsSet noSubject = new JWTClaimsSet.Builder(claims("ana")).subject(null).build();
        String token = BankIssuer.sign(noSubject, BankIssuer.ACCESS_TOKEN, "bank-2026-01", bank.key("bank-2026-01"));
        assertTrue(reason(token).contains("sub"));
    }

    @Test
    void aLongLifetimeIsRefusedEvenWithAGoodSignature() throws Exception {
        String token = bank.accessToken("ana", SCOPE, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, Duration.ofHours(24), Duration.ZERO, "bank-2026-01");
        assertEquals("lifetime longer than the policy allows", reason(token));
    }

    @Test
    void aLifetimeOfExactlyFifteenMinutesIsFine() throws Exception {
        String token = bank.accessToken("ana", SCOPE, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, Duration.ofMinutes(15), Duration.ZERO, "bank-2026-01");
        assertEquals("ana", api.validate(token).subject());
    }

    @Test
    void anAccessTokenCannotBeTakenBackButARefreshTokenCan() throws Exception {
        RefreshTokens refresh = new RefreshTokens();
        String access = bank.accessToken("ana", SCOPE);
        String token = refresh.issue("ana");
        refresh.revokeAll("ana");
        assertEquals("ana", api.validate(access).subject());
        assertNull(refresh.lookup(token));
    }

    @Test
    void theKeySetIsFetchedOnceForManyTokens() throws Exception {
        for (int i = 0; i < 50; i++) {
            api.validate(bank.accessToken("ana", SCOPE));
        }
        assertEquals(1, jwks.requests());
    }

    @Test
    void aTokenSignedByAKeyNobodyPublishedIsRefused() throws Exception {
        ECKey attacker = new ECKeyGenerator(Curve.P_256).keyID("attacker-key").generate();
        String token = BankIssuer.sign(claims("mallory"), BankIssuer.ACCESS_TOKEN, "attacker-key", attacker);
        assertTrue(reason(token).contains("no matching key"));
    }

    @Test
    void anotherKeyUnderTheBanksKeyIdIsRefused() throws Exception {
        ECKey attacker = new ECKeyGenerator(Curve.P_256).keyID("bank-2026-01").generate();
        String token = BankIssuer.sign(claims("mallory"), BankIssuer.ACCESS_TOKEN, "bank-2026-01", attacker);
        assertTrue(reason(token).contains("Invalid signature"));
    }

    @Test
    void aKeySetUrlOrKeyInTheHeaderIsNeverUsed() throws Exception {
        try (JwksServer attackerServer = new JwksServer()) {
            ECKey attacker = new ECKeyGenerator(Curve.P_256).keyID("attacker-key").generate();
            attackerServer.publish(List.of(attacker));
            JWSHeader jku = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key").jwkURL(attackerServer.url().toURI()).build();
            JWSHeader jwk = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key").jwk(attacker.toPublicJWK()).build();
            reason(BankIssuer.sign(claims("mallory"), jku, new ECDSASigner(attacker)));
            reason(BankIssuer.sign(claims("mallory"), jwk, new ECDSASigner(attacker)));
            assertEquals(0, attackerServer.requests());
        }
    }

    @Test
    void manyTokensWithUnknownKeyIdsCauseAtMostOneRefetchInTheMinimumInterval() throws Exception {
        ECKey attacker = new ECKeyGenerator(Curve.P_256).keyID("attacker-key").generate();
        String token = BankIssuer.sign(claims("mallory"), BankIssuer.ACCESS_TOKEN, "attacker-key", attacker);
        api.validate(bank.accessToken("ana", SCOPE));
        for (int i = 0; i < 200; i++) {
            try {
                api.validate(token);
            } catch (Refused expected) {
                // refused, as it should be
            }
        }
        assertTrue(jwks.requests() <= 2, "requests were " + jwks.requests());
    }

    @Test
    void aNewKeyIsTrustedOnlyAfterItIsPublishedAndTheCacheHasExpired() throws Exception {
        AccessTokenValidator quick = new AccessTokenValidator(jwks.url(), 600, 200, clock, BankIssuer.ACCOUNTS_API, SCOPE);
        quick.validate(bank.accessToken("ana", SCOPE));
        bank.createKey("bank-2026-07");
        bank.signWith("bank-2026-07");
        String newToken = bank.accessToken("ana", SCOPE);
        Thread.sleep(300);
        assertTrue(assertThrows(Refused.class, () -> quick.validate(newToken)).getMessage().contains("no matching key"), "not yet published");
        jwks.publish(bank.publicKeys());
        Thread.sleep(700);
        assertEquals("ana", quick.validate(newToken).subject());
    }

    @Test
    void aRetiredKeyStaysTrustedUntilTheCacheExpires() throws Exception {
        AccessTokenValidator quick = new AccessTokenValidator(jwks.url(), 600, 200, clock, BankIssuer.ACCOUNTS_API, SCOPE);
        String old = bank.accessToken("ana", SCOPE);
        assertEquals("ana", quick.validate(old).subject());
        bank.createKey("bank-2026-07");
        bank.signWith("bank-2026-07");
        bank.retire("bank-2026-01");
        jwks.publish(bank.publicKeys());
        assertEquals("ana", quick.validate(old).subject(), "still in the cache");
        Thread.sleep(800);
        assertTrue(assertThrows(Refused.class, () -> quick.validate(old)).getMessage().contains("no matching key"));
    }
}
