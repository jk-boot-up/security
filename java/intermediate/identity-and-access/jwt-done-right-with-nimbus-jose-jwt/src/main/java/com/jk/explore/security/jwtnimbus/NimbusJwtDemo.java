package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * Runs the bank's token story in six numbered acts with Nimbus JOSE + JWT and a real key-set server on the loopback interface. Each act
 * prints its input and its result in separate labelled sections. The keys are fixed and the clock is simulated; an ES256 signature
 * uses a fresh random number, so the signature part is the one value printed as a placeholder.
 */
public class NimbusJwtDemo {

    static final String ACCOUNTS_READ = "accounts:read";
    static final String OLD_KID = "bank-2026-01";
    static final String NEW_KID = "bank-2026-07";
    static final long CACHE_MILLIS = 2_000;
    static final long REFETCH_MILLIS = 1_800;
    static final long WAIT_MILLIS = 2_300;
    private static final int WIDTH = 66;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        act1Build(out);
        act2Parse(out);
        act3Algorithm(out);
        act4Claims(out);
        act5Keys(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One line, or several when it is long: the words carry on under the first word after the lead. */
    private static void wrapped(StringBuilder out, String lead, String text) {
        String indent = " ".repeat(lead.length());
        StringBuilder current = new StringBuilder(lead);
        for (String word : text.split(" ")) {
            if (current.length() > indent.length() && current.length() + 1 + word.length() > WIDTH + 2) {
                line(out, current.toString());
                current = new StringBuilder(indent);
            }
            current.append(current.length() > indent.length() ? " " : "").append(word);
        }
        line(out, current.toString());
    }

    /** A long value cut into lines of 56 characters. */
    private static void chunks(StringBuilder out, String text) {
        for (int i = 0; i < text.length(); i += 56) {
            line(out, "      " + text.substring(i, Math.min(text.length(), i + 56)));
        }
    }

    static String verdict(AccessTokenValidator validator, String token) {
        try {
            return "accepted for " + validator.validate(token).subject();
        } catch (Refused r) {
            return "refused (" + r.getMessage() + ")";
        }
    }

    /** A bank with one fixed key, published, and an API that reads the key set from it. */
    private record Setup(SimClock clock, BankIssuer bank, JwksServer jwks, AccessTokenValidator api) implements AutoCloseable {

        static Setup start() throws IOException, JOSEException {
            SimClock clock = new SimClock();
            BankIssuer bank = new BankIssuer(clock);
            bank.addKey(DemoKeys.fromSeed(OLD_KID, "bank-2026-01 demo seed"));
            bank.signWith(OLD_KID);
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

    private static String json(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static void act1Build(StringBuilder out) throws Exception {
        line(out, "ONE. The bank's authorization server signs ana's token with Nimbus.");
        try (Setup s = Setup.start()) {
            String token = s.bank().accessToken("ana", ACCOUNTS_READ);
            String[] parts = token.split("\\.");
            String header = json(parts[0]);
            String claims = json(parts[1]);
            line(out, "  THE HEADER (JWSHeader as JSON, " + header.length() + " bytes):");
            line(out, "    " + header);
            line(out, "  THE CLAIMS (JWTClaimsSet as JSON, " + claims.length() + " bytes):");
            for (String member : claims.substring(1, claims.length() - 1).split(",(?=\")")) {
                line(out, "    " + member);
            }
            line(out, "  STEP 1. BASE64URL OF EACH (3 bytes -> 4 characters):");
            line(out, "    header " + header.length() + " bytes -> " + parts[0].length() + " characters, claims "
                    + claims.length() + " bytes -> " + parts[1].length());
            byte[] input = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            line(out, "  STEP 2. ES256 = ECDSA ON CURVE P-256 OVER SHA-256:");
            line(out, "    signing input: " + input.length + " ASCII bytes (header part . claims part)");
            line(out, "    SHA-256 of it: " + HexFormat.of().formatHex(digest, 0, 16) + "...");
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            line(out, "    signature: R and S, 32 bytes each = " + signature.length + " bytes -> " + parts[2].length() + " characters");
            line(out, "  THE TOKEN (" + token.length() + " characters = " + parts[0].length() + " + 1 + " + parts[1].length()
                    + " + 1 + " + parts[2].length() + "):");
            line(out, "    PART 1, THE HEADER:");
            chunks(out, parts[0]);
            line(out, "    PART 2, THE CLAIMS:");
            chunks(out, parts[1]);
            line(out, "    PART 3, THE SIGNATURE:");
            line(out, "      <" + parts[2].length() + " characters, new on every run>");
            String again = s.bank().accessToken("ana", ACCOUNTS_READ);
            ECDSAVerifier verifier = new ECDSAVerifier(s.bank().key(OLD_KID).toECPublicKey());
            line(out, "  THE SAME CLAIMS SIGNED TWICE (ECDSA takes a fresh random number):");
            line(out, "    the same first two parts: " + again.startsWith(parts[0] + "." + parts[1] + "."));
            line(out, "    the same signature: " + again.equals(token));
            line(out, "    both verify with the bank's public key: "
                    + (SignedJWT.parse(token).verify(verifier) && SignedJWT.parse(again).verify(verifier)));
        }
    }

    private static void act2Parse(StringBuilder out) throws Exception {
        line(out, "TWO. Parsing is not verifying.");
        try (Setup s = Setup.start()) {
            String token = s.bank().accessToken("ana", ACCOUNTS_READ);
            String[] parts = token.split("\\.");
            String edited = json(parts[1]).replace("\"sub\":\"ana\"", "\"sub\":\"mallory\"");
            String editedPart = Base64.getUrlEncoder().withoutPadding().encodeToString(edited.getBytes(StandardCharsets.UTF_8));
            String forged = parts[0] + "." + editedPart + "." + parts[2];
            line(out, "  THE EDIT (only the claims part is replaced):");
            line(out, "    \"sub\":\"ana\"  ->  \"sub\":\"mallory\"");
            line(out, "  THE TOKEN SENT:");
            line(out, "    part 1, the header     " + parts[0].length() + " characters, unchanged");
            line(out, "    part 2, the claims     " + editedPart.length() + " characters, was " + parts[1].length());
            line(out, "    part 3, the signature  " + parts[2].length() + " characters, ana's, unchanged");
            line(out, "  SignedJWT.parse ALONE (reads the claims, checks nothing):");
            line(out, "    getSubject() -> " + SignedJWT.parse(forged).getJWTClaimsSet().getSubject() + "  <- PARSING IS NOT VERIFYING");
            line(out, "  THE VALIDATOR (DefaultJWTProcessor, signature first):");
            line(out, "    verify with " + OLD_KID + "'s public key -> "
                    + SignedJWT.parse(forged).verify(new ECDSAVerifier(s.bank().key(OLD_KID).toECPublicKey())));
            wrapped(out, "    -> ", verdict(s.api(), forged));
            line(out, "  anyone can read the claims: base64url is an encoding, not");
            line(out, "  encryption, so no secret belongs in them");
        }
    }

    private static void act3Algorithm(StringBuilder out) throws Exception {
        line(out, "THREE. The server chooses the algorithm, never the token.");
        try (Setup s = Setup.start()) {
            line(out, "  THE API'S SETTINGS (in AccessTokenValidator, never from a token):");
            line(out, "    JWSVerificationKeySelector: ES256 only");
            line(out, "    DefaultJOSEObjectTypeVerifier: at+jwt only");
            String good = s.bank().accessToken("ana", ACCOUNTS_READ);
            int dot = good.lastIndexOf('.');
            Signature jdk = Signature.getInstance("SHA256withECDSAinP1363Format");
            jdk.initVerify(s.bank().key(OLD_KID).toECPublicKey());
            jdk.update(good.substring(0, dot).getBytes(StandardCharsets.US_ASCII));
            line(out, "  CHECKED WITHOUT NIMBUS (the JDK's own ECDSA on P-256):");
            line(out, "    SHA256withECDSAinP1363Format verifies ana's token: "
                    + jdk.verify(Base64.getUrlDecoder().decode(good.substring(dot + 1))));
            JWTClaimsSet claims = claims(s.clock(), "ana");
            byte[] publicKeyBytes = s.bank().key(OLD_KID).toPublicJWK().toECPublicKey().getEncoded();
            List<String> tokens = List.of(
                    good,
                    new PlainJWT(new PlainHeader.Builder().type(BankIssuer.ACCESS_TOKEN).build(), claims).serialize(),
                    BankIssuer.sign(claims, new JWSHeader.Builder(JWSAlgorithm.HS256).type(BankIssuer.ACCESS_TOKEN).keyID(OLD_KID).build(),
                            new MACSigner(publicKeyBytes)),
                    BankIssuer.sign(claims, new JOSEObjectType("id+jwt"), OLD_KID, s.bank().key(OLD_KID)));
            line(out, "  THE HEADERS SENT (alg, typ), all with ana's claims:");
            line(out, "    1. ES256  at+jwt  ana's token, signed by the bank");
            line(out, "    2. none   at+jwt  no signature at all");
            line(out, "    3. HS256  at+jwt  the bank's public key used as the secret");
            line(out, "    4. ES256  id+jwt  an ID token offered as an access token");
            line(out, "  THE RESULTS:");
            for (int i = 0; i < tokens.size(); i++) {
                wrapped(out, "    " + (i + 1) + ". ", verdict(s.api(), tokens.get(i)));
            }
        }
    }

    private static JWTClaimsSet claims(SimClock clock, String sub) {
        return new JWTClaimsSet.Builder().issuer(BankIssuer.ISSUER).subject(sub).audience(BankIssuer.ACCOUNTS_API).claim("scope", ACCOUNTS_READ)
                .issueTime(Date.from(clock.instant())).notBeforeTime(Date.from(clock.instant()))
                .expirationTime(Date.from(clock.instant().plus(Duration.ofMinutes(5)))).build();
    }

    private static void act4Claims(StringBuilder out) throws Exception {
        line(out, "FOUR. Every claim that matters is checked.");
        try (Setup s = Setup.start()) {
            BankIssuer bank = s.bank();
            AccessTokenValidator api = s.api();
            long iat = SimClock.START.getEpochSecond();
            long exp = iat + 300;
            line(out, "  THE API EXPECTS (DefaultJWTClaimsVerifier, plus 2 checks of ours):");
            line(out, "    iss " + BankIssuer.ISSUER + ", aud " + BankIssuer.ACCOUNTS_API);
            line(out, "    clock skew " + AccessTokenValidator.CLOCK_SKEW_SECONDS + " seconds; ours: scope " + ACCOUNTS_READ
                    + ", lifetime");
            line(out, "  THE TIMES IN ana's TOKEN (seconds since 1970-01-01 UTC):");
            line(out, "    iat " + iat + "   nbf " + iat + "   exp " + exp);
            line(out, "    accepted while now <= exp + " + AccessTokenValidator.CLOCK_SKEW_SECONDS + " = "
                    + (exp + AccessTokenValidator.CLOCK_SKEW_SECONDS));
            String token = bank.accessToken("ana", ACCOUNTS_READ);
            line(out, "  THE MOMENTS TRIED:");
            line(out, "    1. now " + iat + "  (the moment of issue)");
            line(out, "    2. now " + (exp + 20) + "  (exp + 20)");
            line(out, "    3. now " + (exp + 31) + "  (exp + 31)");
            line(out, "  THE RESULTS:");
            line(out, "    1. " + verdict(api, token));
            s.clock().advance(Duration.ofSeconds(320));
            line(out, "    2. " + verdict(api, token));
            s.clock().advance(Duration.ofSeconds(11));
            line(out, "    3. " + verdict(api, token));
            s.clock().set(SimClock.START);
            Duration five = Duration.ofMinutes(5);
            List<String> changed = List.of(
                    bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.ACCOUNTS_API, five, Duration.ofMinutes(2), OLD_KID),
                    bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, "https://auth.partner.example", BankIssuer.ACCOUNTS_API, five, Duration.ZERO, OLD_KID),
                    bank.accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER, BankIssuer.LOYALTY_API, five, Duration.ZERO, OLD_KID),
                    bank.accessToken("ana", "cards:read"));
            line(out, "  ONE CLAIM CHANGED PER TOKEN (signed by the bank, now " + iat + "):");
            line(out, "    1. nbf   " + (iat + 120) + "  (starts in 2 minutes)");
            line(out, "    2. iss   https://auth.partner.example");
            line(out, "    3. aud   " + BankIssuer.LOYALTY_API);
            line(out, "    4. scope cards:read");
            line(out, "  THE RESULTS:");
            for (int i = 0; i < changed.size(); i++) {
                line(out, "    " + (i + 1) + ". " + verdict(api, changed.get(i)));
            }
            line(out, "  the signature proves who wrote the token; only the claims say");
            line(out, "  whether it is for this API, now");
        }
    }

    private static void act5Keys(StringBuilder out) throws Exception {
        line(out, "FIVE. Keys by id, from the one URL the API was configured with.");
        try (JwksServer attacker = new JwksServer(); JwksServer jwks = new JwksServer()) {
            SimClock clock = new SimClock();
            BankIssuer bank = new BankIssuer(clock);
            bank.addKey(DemoKeys.fromSeed(OLD_KID, "bank-2026-01 demo seed"));
            bank.signWith(OLD_KID);
            jwks.publish(bank.publicKeys());
            AccessTokenValidator api = new AccessTokenValidator(jwks.url(), CACHE_MILLIS, REFETCH_MILLIS, clock, BankIssuer.ACCOUNTS_API, ACCOUNTS_READ);
            line(out, "  THE KEY SET AT http://127.0.0.1:<port>/jwks.json (kid, x):");
            for (ECKey key : bank.publicKeys()) {
                line(out, "    " + key.getKeyID() + "  " + key.getX());
            }
            line(out, "  THE API'S KEY SOURCE (JWKSourceBuilder):");
            line(out, "    only that URL; cache " + CACHE_MILLIS + " ms; at most 1 refetch per " + REFETCH_MILLIS + " ms");

            String oldToken = bank.accessToken("ana", ACCOUNTS_READ);
            ECKey attackerKey = DemoKeys.fromSeed("attacker-key", "a key the bank never published");
            attacker.publish(List.of(attackerKey));
            String unknown = BankIssuer.sign(claims(clock, "mallory"), BankIssuer.ACCESS_TOKEN, "attacker-key", attackerKey);
            String wrongKey = BankIssuer.sign(claims(clock, "mallory"), BankIssuer.ACCESS_TOKEN, OLD_KID, attackerKey);
            JWSHeader withJku = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key")
                    .jwkURL(attacker.url().toURI()).build();
            String jku = BankIssuer.sign(claims(clock, "mallory"), withJku, new ECDSASigner(attackerKey));
            JWSHeader withJwk = new JWSHeader.Builder(JWSAlgorithm.ES256).type(BankIssuer.ACCESS_TOKEN).keyID("attacker-key")
                    .jwk(attackerKey.toPublicJWK()).build();
            String embedded = BankIssuer.sign(claims(clock, "mallory"), withJwk, new ECDSASigner(attackerKey));

            line(out, "  THE TOKENS SENT:");
            line(out, "    1. kid " + OLD_KID + ", ana's token, then 100 more");
            line(out, "    2. kid attacker-key, then the same 100 more times");
            line(out, "    3. kid " + OLD_KID + ", signed with the attacker's key");
            line(out, "    4. jku in the header: the attacker's own key set URL");
            line(out, "    5. jwk in the header: the attacker's public key itself");
            List<String> results = new ArrayList<>();
            String first = verdict(api, oldToken);
            for (int i = 0; i < 100; i++) {
                verdict(api, bank.accessToken("ana", ACCOUNTS_READ));
            }
            results.add(first + "; fetches so far: " + jwks.requests());
            String second = verdict(api, unknown);
            int before = jwks.requests();
            for (int i = 0; i < 100; i++) {
                verdict(api, unknown);
            }
            results.add(second + "; fetches: " + before + ", then " + (jwks.requests() - before) + " more");
            results.add(verdict(api, wrongKey));
            results.add(verdict(api, jku) + "; the attacker's server was asked " + attacker.requests() + " times");
            results.add(verdict(api, embedded));
            line(out, "  THE RESULTS:");
            for (int i = 0; i < results.size(); i++) {
                wrapped(out, "    " + (i + 1) + ". ", results.get(i));
            }
            line(out, "  4 and 5: jku and jwk are never followed; only the kid is looked up");

            bank.addKey(DemoKeys.fromSeed(NEW_KID, "bank-2026-07 demo seed"));
            jwks.publish(bank.publicKeys());
            Thread.sleep(WAIT_MILLIS);
            List<String> rotation = new ArrayList<>();
            rotation.add(verdict(api, oldToken) + "; fetches: " + jwks.requests());
            bank.signWith(NEW_KID);
            rotation.add(verdict(api, bank.accessToken("ana", ACCOUNTS_READ)));
            bank.retire(OLD_KID);
            jwks.publish(bank.publicKeys());
            rotation.add(verdict(api, oldToken) + "  <- STILL IN THE CACHE");
            Thread.sleep(WAIT_MILLIS);
            rotation.add(verdict(api, oldToken) + "; fetches: " + jwks.requests());
            line(out, "  THE ROTATION (each step waits past the " + CACHE_MILLIS + " ms cache time):");
            line(out, "    6. " + NEW_KID + " is published; the old token is sent");
            line(out, "    7. the bank signs with " + NEW_KID);
            line(out, "    8. " + OLD_KID + " is retired; at once, the old token");
            line(out, "    9. after the cache time, the old token");
            line(out, "  THE RESULTS:");
            for (int i = 0; i < rotation.size(); i++) {
                wrapped(out, "    " + (i + 6) + ". ", rotation.get(i));
            }
            line(out, "  every one of the " + jwks.requests() + " fetches went to the configured URL");
        }
    }

    private static void act6Bill(StringBuilder out) throws Exception {
        line(out, "SIX. The bill: a token cannot be taken back.");
        try (Setup s = Setup.start()) {
            RefreshTokens refresh = new RefreshTokens();
            String access = s.bank().accessToken("ana", ACCOUNTS_READ);
            String refreshToken = refresh.issue("ana");
            line(out, "  WHAT ana HOLDS (seconds after issue):");
            line(out, "    the access token, which stops working after 300 + 30 = 330");
            line(out, "    a refresh token <43 random characters>, kept by the bank");
            line(out, "  THE EVENT:");
            line(out, "    at 120 the bank removes ana's access and revokes the refresh token");
            s.clock().advance(Duration.ofSeconds(120));
            refresh.revokeAll("ana");
            s.clock().advance(Duration.ofSeconds(120));
            line(out, "  THE RESULTS:");
            line(out, "    at 240 the access token:  " + verdict(s.api(), access) + "  <- STILL WORKS");
            line(out, "    at 240 the refresh token: " + (refresh.lookup(refreshToken) == null ? "refused (revoked)" : "accepted"));
            s.clock().advance(Duration.ofSeconds(91));
            line(out, "    at 331 the access token:  " + verdict(s.api(), access));
            s.clock().set(SimClock.START);
            line(out, "  THE POLICY THAT FOLLOWS (ours: at most "
                    + AccessTokenValidator.MAX_LIFETIME.toSeconds() + " seconds = 15 minutes):");
            line(out, "    a token with exp = iat + 86400 (24 hours):");
            line(out, "    -> " + verdict(s.api(), s.bank().accessToken("ana", ACCOUNTS_READ, BankIssuer.ACCESS_TOKEN, BankIssuer.ISSUER,
                    BankIssuer.ACCOUNTS_API, Duration.ofHours(24), Duration.ZERO, OLD_KID)));
            line(out, "  the library parses, verifies and checks claims; the policy is ours");
        }
    }
}
