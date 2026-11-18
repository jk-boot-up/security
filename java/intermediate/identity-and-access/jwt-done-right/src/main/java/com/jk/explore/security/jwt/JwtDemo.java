package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the online bank story in six numbered acts and prints exact, repeatable results. Each act prints its input and its result in
 * separate labelled sections. Ed25519 is deterministic and the clock is simulated, so even the tokens and signatures repeat.
 */
public class JwtDemo {

    static final long START = 1_800_000_000L;

    /** RFC 8037 appendix A: the published Ed25519 test key, its public half, and its example JWS. */
    static final String RFC8037_SEED = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";
    static final String RFC8037_X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo";
    static final String RFC8037_TOKEN = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc."
            + "hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";

    static final String OLD_KID = "bank-2026-01";
    static final String NEW_KID = "bank-2026-07";

    /** The bank's first signing key is the RFC 8037 test key, so anyone can check its signatures; the others are demo keys from fixed seeds. */
    static final KeyPair OLD_KEY = Ed25519.fromSeed(HexFormat.of().parseHex(RFC8037_SEED));
    static final KeyPair NEW_KEY = Ed25519.fromSeed(seed("bank-2026-07 demo seed"));
    static final KeyPair INTRUDER_KEY = Ed25519.fromSeed(seed("a key the bank never published"));

    private static final int WRAP = 56;

    static byte[] seed(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Build(out);
        act2Decode(out);
        act3Algorithm(out);
        act4Claims(out);
        act5Keys(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A long value over several lines of at most WRAP characters, each indented by six spaces. */
    private static void wrapped(StringBuilder out, String text) {
        for (int i = 0; i < text.length(); i += WRAP) {
            line(out, "      " + text.substring(i, Math.min(text.length(), i + WRAP)));
        }
    }

    static KeySet bankKeys() {
        return new KeySet().add(OLD_KID, OLD_KEY.getPublic());
    }

    static String outcome(Validator validator, String token) {
        try {
            AccessToken accepted = validator.validate(token);
            return "accepted for " + accepted.subject();
        } catch (InvalidTokenException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static String anaToken() {
        return Issuer.issue(OLD_KID, OLD_KEY.getPrivate(), "ana", "accounts:read", START, 300);
    }

    private static String part(String token, int index) {
        return token.split("\\.", -1)[index];
    }

    private static String utc(long seconds) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.ofEpochSecond(seconds).atOffset(ZoneOffset.UTC)) + " UTC";
    }

    private static void act1Build(StringBuilder out) {
        line(out, "ONE. The bank's authorization server builds ana's access token.");
        Map<String, Object> header = Issuer.header(OLD_KID);
        Map<String, Object> claims = Issuer.claims("ana", "accounts:read", START, 300);
        String headerJson = Json.write(header);
        String claimsJson = Json.write(claims);
        line(out, "  THE HEADER (JSON, " + headerJson.length() + " bytes):");
        line(out, "    " + headerJson);
        line(out, "  THE CLAIMS (JSON, " + claimsJson.length() + " bytes, one member per line):");
        for (Map.Entry<String, Object> claim : claims.entrySet()) {
            String member = String.format("%-28s", Json.write(Map.of(claim.getKey(), claim.getValue())).replaceAll("^\\{|\\}$", ""));
            String note = switch (claim.getKey()) {
                case "iat" -> "issued at " + utc(START);
                case "nbf" -> "not before: the same second";
                case "exp" -> "iat + 300 seconds = 5 minutes";
                default -> "";
            };
            line(out, ("    " + member + note).stripTrailing());
        }
        String headerPart = Base64Url.encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String claimsPart = Base64Url.encode(claimsJson.getBytes(StandardCharsets.UTF_8));
        byte[] first = Arrays.copyOf(headerJson.getBytes(StandardCharsets.UTF_8), 3);
        line(out, "  STEP 1. BASE64URL: EVERY 3 BYTES BECOME 4 CHARACTERS:");
        line(out, "    " + headerJson.substring(0, 3) + "  = bytes " + HexFormat.ofDelimiter(" ").formatHex(first));
        line(out, "        = bits " + Base64UrlSteps.groups(first));
        line(out, "        = characters " + String.join(" ", Base64UrlSteps.encode(first).split("")));
        line(out, "    header " + headerJson.length() + " bytes -> " + headerPart.length() + " characters, claims "
                + claimsJson.length() + " bytes -> " + claimsPart.length());
        String signingInput = headerPart + "." + claimsPart;
        line(out, "  STEP 2. THE SIGNING INPUT (header part, a dot, claims part):");
        line(out, "    " + signingInput.length() + " ASCII bytes, starting " + signingInput.substring(0, 24) + "...");
        byte[] signature = Ed25519.sign(OLD_KEY.getPrivate(), signingInput.getBytes(StandardCharsets.US_ASCII));
        line(out, "  STEP 3. ED25519 SIGNS IT WITH THE PRIVATE KEY " + OLD_KID + ":");
        line(out, "    " + signature.length + " bytes, starting " + HexFormat.of().formatHex(signature, 0, 16) + "...");
        line(out, "    base64url -> " + Base64Url.encode(signature).length() + " characters");
        String token = signingInput + "." + Base64Url.encode(signature);
        line(out, "  THE TOKEN (" + token.length() + " characters = " + headerPart.length() + " + 1 + " + claimsPart.length()
                + " + 1 + " + Base64Url.encode(signature).length() + "):");
        line(out, "    PART 1, THE HEADER:");
        wrapped(out, headerPart);
        line(out, "    PART 2, THE CLAIMS:");
        wrapped(out, claimsPart);
        line(out, "    PART 3, THE SIGNATURE:");
        wrapped(out, Base64Url.encode(signature));
        line(out, "  the same as the issuer's output: " + token.equals(anaToken()));
    }

    private static void act2Decode(StringBuilder out) {
        line(out, "TWO. Decoding is not verifying.");
        String token = anaToken();
        Map<String, Object> edited = Issuer.claims("mallory", "accounts:read", START, 300);
        String editedPart = Base64Url.encode(Json.write(edited).getBytes(StandardCharsets.UTF_8));
        String forged = part(token, 0) + "." + editedPart + "." + part(token, 2);
        line(out, "  THE EDIT (only the claims part is replaced):");
        line(out, "    \"sub\":\"ana\"  ->  \"sub\":\"mallory\"");
        line(out, "  THE TOKEN SENT:");
        line(out, "    part 1, the header     " + part(forged, 0).length() + " characters, unchanged");
        line(out, "    part 2, the claims     " + editedPart.length() + " characters, was " + part(token, 1).length());
        line(out, "    part 3, the signature  " + part(forged, 2).length() + " characters, ana's, unchanged");
        SimClock clock = new SimClock(START + 10);
        line(out, "  A READER THAT ONLY DECODES (splits, decodes part 2, reads sub):");
        line(out, "    -> " + new NaiveReader(clock).subject(forged).orElse("nobody") + "  <- DECODING IS NOT VERIFYING");
        line(out, "  THE VALIDATOR (verifies part 3 over parts 1 and 2 first):");
        try {
            Jws.Parts parts = Jws.parse(forged);
            line(out, "    Ed25519 verify with " + OLD_KID + "'s public key -> "
                    + Ed25519.verify(OLD_KEY.getPublic(), parts.signingInput(), parts.signature()));
        } catch (InvalidTokenException e) {
            line(out, "    the token did not parse: " + e.getMessage());
        }
        line(out, "    -> " + outcome(new Validator(Policy.bank(), bankKeys(), clock), forged));
        line(out, "  anyone can read the claims: base64url is an encoding, not");
        line(out, "  encryption, so no secret belongs in them");
    }

    private static void act3Algorithm(StringBuilder out) {
        line(out, "THREE. The server chooses the algorithm, never the token.");
        line(out, "  THE API'S SETTINGS (fixed in Policy, never read from a token):");
        line(out, "    algorithm " + Policy.ALGORITHM + ", type " + Policy.TYPE);
        line(out, "  CHECKED AGAINST THE PUBLISHED EXAMPLE (RFC 8037, appendix A):");
        try {
            Jws.Parts example = Jws.parse(RFC8037_TOKEN);
            boolean verifies = Ed25519.verify(Ed25519.publicKey(Base64Url.decode(RFC8037_X)), example.signingInput(), example.signature());
            boolean sameAgain = Arrays.equals(example.signature(), Ed25519.sign(OLD_KEY.getPrivate(), example.signingInput()));
            line(out, "    the example verifies with its published public key: " + verifies);
            line(out, "    signing it again gives the same 64 bytes: " + sameAgain);
            line(out, "    the public key derived from the private key matches: "
                    + Base64Url.encode(Ed25519.encode(OLD_KEY.getPublic())).equals(RFC8037_X));
        } catch (InvalidTokenException e) {
            line(out, "    the example did not parse: " + e.getMessage());
        }
        Validator validator = new Validator(Policy.bank(), bankKeys(), new SimClock(START + 10));
        String token = anaToken();
        Map<String, Object> none = new LinkedHashMap<>(Issuer.header(OLD_KID));
        none.put("alg", "none");
        String unsigned = Base64Url.encode(Json.write(none).getBytes(StandardCharsets.UTF_8)) + "." + part(token, 1) + ".";
        Map<String, Object> hs = new LinkedHashMap<>(Issuer.header(OLD_KID));
        hs.put("alg", "HS256");
        Map<String, Object> idToken = new LinkedHashMap<>(Issuer.header(OLD_KID));
        idToken.put("typ", "id+jwt");
        Map<String, Object> claims = Issuer.claims("ana", "accounts:read", START, 300);
        List<String> tokens = List.of(token, unsigned, Issuer.issue(OLD_KEY.getPrivate(), hs, claims),
                Issuer.issue(OLD_KEY.getPrivate(), idToken, claims));
        line(out, "  THE HEADERS SENT (alg, typ), all with ana's claims:");
        line(out, "    1. EdDSA  at+jwt  ana's token, signed by the bank");
        line(out, "    2. none   at+jwt  no signature at all");
        line(out, "    3. HS256  at+jwt  another algorithm, signed by the bank");
        line(out, "    4. EdDSA  id+jwt  an ID token offered as an access token");
        line(out, "  THE RESULTS:");
        for (int i = 0; i < tokens.size(); i++) {
            line(out, "    " + (i + 1) + ". " + outcome(validator, tokens.get(i)));
        }
        line(out, "  the header is only checked against the API's own settings");
    }

    private static void act4Claims(StringBuilder out) {
        line(out, "FOUR. Every claim that matters is checked.");
        Policy policy = Policy.bank();
        line(out, "  THE API EXPECTS:");
        line(out, "    iss " + policy.issuer() + ", aud " + policy.audience());
        line(out, "    scope " + policy.requiredScope() + ", clock skew " + policy.skewSeconds() + " seconds");
        line(out, "  THE TIMES IN ana's TOKEN (seconds since 1970-01-01 UTC):");
        line(out, "    iat " + START + "   nbf " + START + "   exp " + (START + 300));
        line(out, "    accepted while now < exp + " + policy.skewSeconds() + " = " + (START + 300 + policy.skewSeconds()));
        SimClock clock = new SimClock(START);
        Validator validator = new Validator(policy, bankKeys(), clock);
        String token = anaToken();
        line(out, "  THE MOMENTS TRIED:");
        line(out, "    1. now " + START + "  (the moment of issue)");
        line(out, "    2. now " + (START + 320) + "  (exp + 20)");
        line(out, "    3. now " + (START + 330) + "  (exp + 30)");
        line(out, "  THE RESULTS:");
        line(out, "    1. " + outcome(validator, token));
        clock.advance(320);
        line(out, "    2. " + outcome(validator, token));
        clock.advance(10);
        line(out, "    3. " + outcome(validator, token));
        Validator now = new Validator(policy, bankKeys(), new SimClock(START));
        Map<String, Object> later = Issuer.claims("ana", "accounts:read", START, 300);
        later.put("nbf", START + 120);
        Map<String, Object> otherIssuer = Issuer.claims("ana", "accounts:read", START, 300);
        otherIssuer.put("iss", "https://auth.partner.example");
        Map<String, Object> otherAudience = Issuer.claims("ana", "accounts:read", START, 300);
        otherAudience.put("aud", "loyalty-api");
        List<Map<String, Object>> changed = List.of(later, otherIssuer, otherAudience,
                Issuer.claims("ana", "cards:read", START, 300));
        line(out, "  ONE CLAIM CHANGED PER TOKEN (signed by the bank, now " + START + "):");
        line(out, "    1. nbf   " + (START + 120) + "  (starts in 2 minutes)");
        line(out, "    2. iss   https://auth.partner.example");
        line(out, "    3. aud   loyalty-api");
        line(out, "    4. scope cards:read");
        line(out, "  THE RESULTS:");
        for (int i = 0; i < changed.size(); i++) {
            line(out, "    " + (i + 1) + ". " + outcome(now, Issuer.issue(OLD_KEY.getPrivate(), Issuer.header(OLD_KID), changed.get(i))));
        }
        line(out, "  the signature proves who wrote the token; only the claims say");
        line(out, "  whether it is for this API, now");
    }

    private static void act5Keys(StringBuilder out) {
        line(out, "FIVE. Keys by id, from a set the API already holds.");
        KeySet keys = bankKeys().add(NEW_KID, NEW_KEY.getPublic());
        Validator validator = new Validator(Policy.bank(), keys, new SimClock(START + 10));
        line(out, "  THE KEY SET THE API HOLDS (kid and public key x):");
        line(out, "    " + OLD_KID + "  " + Base64Url.encode(Ed25519.encode(OLD_KEY.getPublic())));
        line(out, "    " + NEW_KID + "  " + Base64Url.encode(Ed25519.encode(NEW_KEY.getPublic())));
        String old = anaToken();
        String fresh = Issuer.issue(NEW_KID, NEW_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        line(out, "  THE TOKENS SENT (the kid in the header, the key that signed):");
        line(out, "    1. kid " + OLD_KID + "   signed with " + OLD_KID);
        line(out, "    2. kid " + NEW_KID + "   signed with " + NEW_KID);
        line(out, "  THE RESULTS:");
        line(out, "    1. " + outcome(validator, old));
        line(out, "    2. " + outcome(validator, fresh));
        keys.remove(OLD_KID);
        line(out, "  THE KEY SET AFTER " + OLD_KID + " IS RETIRED:");
        line(out, "    " + String.join(", ", keys.ids()));
        line(out, "  THE TOKENS SENT:");
        line(out, "    3. kid " + OLD_KID + "   signed with " + OLD_KID);
        line(out, "    4. kid attacker-key   signed with an intruder's key");
        line(out, "    5. kid " + NEW_KID + "   signed with an intruder's key");
        line(out, "  THE RESULTS:");
        line(out, "    3. " + outcome(validator, old));
        line(out, "    4. " + outcome(validator, Issuer.issue("attacker-key", INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300)));
        line(out, "    5. " + outcome(validator, Issuer.issue(NEW_KID, INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300)));
        line(out, "  the API never fetches a key because a token asked it to");
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: a token cannot be taken back.");
        SimClock clock = new SimClock(START);
        Validator validator = new Validator(Policy.bank(), bankKeys(), clock);
        RefreshTokens refresh = new RefreshTokens();
        String access = anaToken();
        String handle = refresh.issue("ana");
        line(out, "  WHAT ana HOLDS (seconds after issue):");
        line(out, "    the access token, which stops working at 300 + 30 of skew = 330");
        line(out, "    the refresh token " + handle + ", which the bank looks up each time");
        clock.advance(120);
        refresh.revokeAll("ana");
        line(out, "  THE EVENT:");
        line(out, "    at 120 the bank removes ana's access and revokes " + handle);
        clock.advance(120);
        line(out, "  THE RESULTS:");
        line(out, "    at 240 the access token:  " + outcome(validator, access) + "  <- STILL WORKS");
        line(out, "    at 240 the refresh token: " + (refresh.redeem(handle).isPresent() ? "accepted" : "refused (revoked)"));
        clock.advance(90);
        line(out, "    at 330 the access token:  " + outcome(validator, access));
        line(out, "  THE POLICY THAT FOLLOWS (at most " + Policy.bank().maxLifetimeSeconds() + " seconds = 15 minutes):");
        line(out, "    a token with exp = iat + 3600:");
        line(out, "    -> " + outcome(new Validator(Policy.bank(), bankKeys(), new SimClock(START)),
                Issuer.issue(OLD_KID, OLD_KEY.getPrivate(), "ana", "accounts:read", START, 3600)));
        line(out, "  in production, use a maintained library, configured strictly");
    }
}
