package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/** Runs the bank's token story in five numbered acts with Nimbus JOSE + JWT and a real key-set server on the loopback interface. */
public class NimbusJwtDemo {

    static final String ACCOUNTS_READ = "accounts:read";
    static final long CACHE_MILLIS = 2_000;
    static final long REFETCH_MILLIS = 1_800;
    static final long WAIT_MILLIS = 2_300;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys and signatures are fresh on every run and are never printed. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        act1DecodingIsNotVerifying(out);
        act2TheServerChoosesTheAlgorithm(out);
        act3EveryClaim(out);
        act4KeysById(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String verdict(AccessTokenValidator validator, String token) {
        try {
            return "accepted for " + validator.validate(token).subject();
        } catch (Refused r) {
            return "refused (" + r.getMessage() + ")";
        }
    }

    /** A bank with one key, published, and an API that reads the key set from it. */
    private record Setup(SimClock clock, BankIssuer bank, JwksServer jwks, AccessTokenValidator api) implements AutoCloseable {

        static Setup start() throws IOException, JOSEException {
            SimClock clock = new SimClock();
            BankIssuer bank = new BankIssuer(clock);
            bank.createKey("bank-2026-01");
            bank.signWith("bank-2026-01");
            JwksServer jwks = new JwksServer();
            jwks.publish(bank.publicKeys());
            AccessTokenValidator api = new AccessTokenValidator(jwks.url(), 300_000, 30_000, clock, BankIssuer.ACCOUNTS_API, ACCOUNTS_READ);
            return new Setup(clock, bank, jwks, api);
        }

        @Override
        public void close() {
            jwks.close();
        }
    }

    private static void act1DecodingIsNotVerifying(StringBuilder out) throws Exception {
        line(out, "ONE. Three parts, and decoding is not verifying.");
        try (Setup s = Setup.start()) {
            String token = s.bank().accessToken("ana", ACCOUNTS_READ);
            SignedJWT jwt = SignedJWT.parse(token);
            line(out, "  the bank's authorization server issues ana an access token of " + token.length() + " characters in " + token.split("\\.").length + " parts");
            line(out, "  header: " + jwt.getHeader().toString());
            line(out, "  claims: " + jwt.getJWTClaimsSet().toString());
            line(out, "  the third part is a " + jwt.getSignature().decode().length + "-byte ES256 signature (R and S, 32 bytes each) over the first two");
            line(out, "  anyone can read the claims: base64url is an encoding, not encryption, so no secret belongs in them");
            String[] parts = token.split("\\.");
            String edited = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace("\"sub\":\"ana\"", "\"sub\":\"mallory\"");
            String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(edited.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
            line(out, "  the claims edited to sub=mallory, with ana's signature kept: a reader that only parses says " + SignedJWT.parse(forged).getJWTClaimsSet().getSubject() + "  <- PARSING IS NOT VERIFYING");
            line(out, "  the validator: " + verdict(s.api(), forged));
        }
    }

    private static void act2TheServerChoosesTheAlgorithm(StringBuilder out) throws Exception {
        line(out, "TWO. The server chooses the algorithm, never the token.");
        try (Setup s = Setup.start()) {
            line(out, "  ana's token with the bank's key: " + verdict(s.api(), s.bank().accessToken("ana", ACCOUNTS_READ)));
            JWTClaimsSet claims = claims(s, "ana", BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, ACCOUNTS_READ, Duration.ofMinutes(5), Duration.ZERO);
            line(out, "  the same claims with a header that says none and no signature: " + verdict(s.api(), new PlainJWT(new PlainHeader.Builder().type(BankIssuer.ACCESS_TOKEN).build(), claims).serialize()));
            byte[] publicKeyBytes = s.bank().key("bank-2026-01").toPublicJWK().toECPublicKey().getEncoded();
            String hs256 = BankIssuer.sign(claims, new JWSHeader.Builder(JWSAlgorithm.HS256).type(BankIssuer.ACCESS_TOKEN).keyID("bank-2026-01").build(), new MACSigner(publicKeyBytes));
            line(out, "  a header that says HS256, signed with the bank's public key as the shared secret: " + verdict(s.api(), hs256));
            line(out, "  a correctly signed token of another type, offered as an access token: "
                    + verdict(s.api(), BankIssuer.sign(claims, new JOSEObjectType("id+jwt"), "bank-2026-01", s.bank().key("bank-2026-01"))));
            line(out, "  the algorithm and the type are settings of the API; the header is only checked against them");
        }
    }

    private static JWTClaimsSet claims(Setup s, String sub, String issuer, String audience, String scope, Duration lifetime, Duration notBeforeOffset) {
        return new JWTClaimsSet.Builder().issuer(issuer).subject(sub).audience(audience).claim("scope", scope).issueTime(Date.from(s.clock().instant()))
                .notBeforeTime(Date.from(s.clock().instant().plus(notBeforeOffset))).expirationTime(Date.from(s.clock().instant().plus(lifetime))).build();
    }

    private static void act3EveryClaim(StringBuilder out) throws Exception {
        line(out, "THREE. Every claim that matters is checked.");
        try (Setup s = Setup.start()) {
            BankIssuer bank = s.bank();
            AccessTokenValidator api = s.api();
            String token = bank.accessToken("ana", ACCOUNTS_READ);
            line(out, "  at the moment of issue: " + verdict(api, token));
            s.clock().advance(Duration.ofMinutes(5).plusSeconds(20));
            line(out, "  20 seconds after exp, inside the 30 seconds allowed for clock skew: " + verdict(api, token));
            s.clock().advance(Duration.ofSeconds(11));
            line(out, "  31 seconds after exp: " + verdict(api, token));
            s.clock().set(SimClock.START);
            line(out, "  a token that starts in 2 minutes, presented now: "
                    + verdict(api, bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, Duration.ofMinutes(5), Duration.ofMinutes(2), "bank-2026-01")));
            line(out, "  issued by another authorization server: "
                    + verdict(api, bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, "https://auth.other.example", BankIssuer.ACCOUNTS_API, Duration.ofMinutes(5), Duration.ZERO, "bank-2026-01")));
            line(out, "  meant for the loyalty API, sent to the accounts API: "
                    + verdict(api, bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.LOYALTY_API, Duration.ofMinutes(5), Duration.ZERO, "bank-2026-01")));
            line(out, "  with only the scope cards:read: " + verdict(api, bank.accessToken("ana", "cards:read")));
            line(out, "  a valid signature proves who wrote the token; only the claims say whether it is for this API, now");
        }
    }

    private static void act4KeysById(StringBuilder out) throws Exception {
        line(out, "FOUR. Keys by id, from the one set the API was configured with.");
        try (JwksServer attacker = new JwksServer()) {
            SimClock clock = new SimClock();
            BankIssuer bank = new BankIssuer(clock);
            bank.createKey("bank-2026-01");
            bank.signWith("bank-2026-01");
            try (JwksServer jwks = new JwksServer()) {
                jwks.publish(bank.publicKeys());
                AccessTokenValidator api = new AccessTokenValidator(jwks.url(), CACHE_MILLIS, REFETCH_MILLIS, clock, BankIssuer.ACCOUNTS_API, ACCOUNTS_READ);
                String oldToken = bank.accessToken("ana", ACCOUNTS_READ);
                line(out, "  the bank publishes 1 key, bank-2026-01; the first token: " + verdict(api, oldToken));
                for (int i = 0; i < 100; i++) {
                    api.validate(bank.accessToken("ana", ACCOUNTS_READ));
                }
                line(out, "  100 more tokens: the key set endpoint was asked " + jwks.requests() + " time in all, the rest came from the cache");

                // Everything an attacker can put in a token, tried straight away, while the key set was fetched a moment ago.
                ECKey attackerKey = new com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256).keyID("attacker-key").generate();
                String unknown = BankIssuer.sign(claims(clock, "mallory"), BankIssuer.ACCESS_TOKEN, "attacker-key", attackerKey);
                line(out, "  a token naming a key id the API has never seen: " + verdict(api, unknown) + "; the key set endpoint has now been asked " + jwks.requests() + " times");
                int before = jwks.requests();
                for (int i = 0; i < 100; i++) {
                    verdict(api, unknown);
                }
                line(out, "  100 more of them: the key set endpoint was asked " + (jwks.requests() - before) + " more times (a minimum time between refetches protects it)");
                String wrongKey = BankIssuer.sign(claims(clock, "mallory"), BankIssuer.ACCESS_TOKEN, "bank-2026-01", attackerKey);
                line(out, "  a token signed with another key but naming the bank's key id: " + verdict(api, wrongKey));
                attacker.publish(List.of(attackerKey));
                JWSHeader withJku = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key").jwkURL(attacker.url().toURI()).build();
                String jku = BankIssuer.sign(claims(clock, "mallory"), withJku, new com.nimbusds.jose.crypto.ECDSASigner(attackerKey));
                JWSHeader withJwk = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key").jwk(attackerKey.toPublicJWK()).build();
                String embedded = BankIssuer.sign(claims(clock, "mallory"), withJwk, new com.nimbusds.jose.crypto.ECDSASigner(attackerKey));
                line(out, "  a token whose header points at the attacker's own key set (jku): " + verdict(api, jku) + "; the attacker's server was asked " + attacker.requests() + " times");
                line(out, "  a token that carries its own public key in the header (jwk): " + verdict(api, embedded));

                bank.createKey("bank-2026-07");
                jwks.publish(bank.publicKeys());
                Thread.sleep(WAIT_MILLIS);
                line(out, "  the bank publishes bank-2026-07 before using it; after the cache time the API trusts 2 keys: the old token " + verdict(api, oldToken) + "; asked " + jwks.requests() + " times");
                bank.signWith("bank-2026-07");
                line(out, "  the bank signs with the new key: " + verdict(api, bank.accessToken("ana", ACCOUNTS_READ)));

                bank.retire("bank-2026-01");
                jwks.publish(bank.publicKeys());
                line(out, "  the old key is retired and no longer published; inside the cache time the old token: " + verdict(api, oldToken) + "  <- A RETIRED KEY LIVES ON IN THE CACHE");
                Thread.sleep(WAIT_MILLIS);
                line(out, "  after the cache time the old token: " + verdict(api, oldToken) + "; asked " + jwks.requests() + " times (the cache expired, and the unknown key id made the library ask once more before giving up)");
                line(out, "  keys come from the URL in the API's settings and from nowhere a token names: every one of the " + jwks.requests() + " fetches was for that URL");
            }
        }
    }

    private static JWTClaimsSet claims(SimClock clock, String sub) {
        return new JWTClaimsSet.Builder().issuer(BankIssuer.ISSUER).subject(sub).audience(BankIssuer.ACCOUNTS_API).claim("scope", ACCOUNTS_READ)
                .issueTime(Date.from(clock.instant())).notBeforeTime(Date.from(clock.instant())).expirationTime(Date.from(clock.instant().plus(Duration.ofMinutes(5)))).build();
    }

    private static void act5Bill(StringBuilder out) throws Exception {
        line(out, "FIVE. The bill: a token cannot be taken back.");
        try (Setup s = Setup.start()) {
            RefreshTokens refresh = new RefreshTokens();
            String access = s.bank().accessToken("ana", ACCOUNTS_READ);
            String refreshToken = refresh.issue("ana");
            line(out, "  ana's access is removed 2 minutes after her token was issued");
            s.clock().advance(Duration.ofMinutes(2));
            refresh.revokeAll("ana");
            line(out, "  2 minutes later her access token: " + verdict(s.api(), access) + "  <- STILL WORKS UNTIL IT EXPIRES");
            line(out, "  her refresh token, which the server looks up: " + (refresh.lookup(refreshToken) == null ? "refused (revoked)" : "accepted"));
            s.clock().advance(Duration.ofMinutes(3).plusSeconds(31));
            line(out, "  at 5 minutes and 31 seconds the access token: " + verdict(s.api(), access));
            s.clock().set(SimClock.START);
            line(out, "  so access tokens live for minutes, and a token asking for more than 15 is refused: "
                    + verdict(s.api(), s.bank().accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, Duration.ofHours(24), Duration.ZERO, "bank-2026-01")));
            line(out, "  where a browser should keep tokens is the tokens-in-the-browser project; the library does the parsing, the signature and the claims, and the policy stays ours");
        }
    }
}
