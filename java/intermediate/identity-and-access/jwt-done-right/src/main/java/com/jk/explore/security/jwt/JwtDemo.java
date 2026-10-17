package com.jk.explore.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. Ed25519 is deterministic, so even the tokens repeat. */
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
        act1Parts(out);
        act2Algorithm(out);
        act3Claims(out);
        act4Keys(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
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

    private static void act1Parts(StringBuilder out) {
        line(out, "ONE. Three parts, and decoding is not verifying.");
        String token = anaToken();
        line(out, "  the bank's authorization server issues ana an access token of " + token.length() + " characters in 3 parts");
        line(out, "  header: " + new String(java.util.Base64.getUrlDecoder().decode(part(token, 0)), StandardCharsets.UTF_8));
        line(out, "  claims: " + new String(java.util.Base64.getUrlDecoder().decode(part(token, 1)), StandardCharsets.UTF_8));
        line(out, "  the third part is a " + java.util.Base64.getUrlDecoder().decode(part(token, 2)).length + "-byte Ed25519 signature over the first two");
        line(out, "  anyone can read the claims: base64url is an encoding, not encryption, so no secret belongs in them");
        Map<String, Object> edited = Issuer.claims("mallory", "accounts:read", START, 300);
        String forged = part(token, 0) + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Json.write(edited).getBytes(StandardCharsets.UTF_8)) + "." + part(token, 2);
        SimClock clock = new SimClock(START + 10);
        line(out, "  the claims edited to sub=mallory, with ana's signature kept: a reader that only decodes says "
                + new NaiveReader(clock).subject(forged).orElse("nobody") + "  <- DECODING IS NOT VERIFYING");
        line(out, "  the validator: " + outcome(new Validator(Policy.bank(), bankKeys(), clock), forged));
    }

    private static void act2Algorithm(StringBuilder out) {
        line(out, "TWO. The server chooses the algorithm, never the token.");
        try {
            Jws.Parts example = Jws.parse(RFC8037_TOKEN);
            boolean verifies = Ed25519.verify(Ed25519.publicKey(Base64Url.decode(RFC8037_X)), example.signingInput(), example.signature());
            boolean sameAgain = java.util.Arrays.equals(example.signature(),
                    Ed25519.sign(OLD_KEY.getPrivate(), example.signingInput()));
            line(out, "  the RFC 8037 example token verifies with its published key: " + verifies
                    + ", and signing it again gives the same signature: " + sameAgain);
            line(out, "  the JDK derives the published public key from the published private key: "
                    + Base64Url.encode(Ed25519.encode(OLD_KEY.getPublic())).equals(RFC8037_X));
        } catch (InvalidTokenException e) {
            line(out, "  the RFC 8037 example did not parse: " + e.getMessage());
        }
        SimClock clock = new SimClock(START + 10);
        Validator validator = new Validator(Policy.bank(), bankKeys(), clock);
        String token = anaToken();
        line(out, "  ana's token with the bank's key: " + outcome(validator, token));
        Map<String, Object> none = new LinkedHashMap<>(Issuer.header(OLD_KID));
        none.put("alg", "none");
        String unsigned = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(Json.write(none).getBytes(StandardCharsets.UTF_8))
                + "." + part(token, 1) + ".";
        line(out, "  the same claims with a header that says none and no signature: " + outcome(validator, unsigned));
        Map<String, Object> hs = new LinkedHashMap<>(Issuer.header(OLD_KID));
        hs.put("alg", "HS256");
        line(out, "  a header that says HS256: " + outcome(validator, Issuer.issue(OLD_KEY.getPrivate(), hs, Issuer.claims("ana", "accounts:read", START, 300))));
        Map<String, Object> idToken = new LinkedHashMap<>(Issuer.header(OLD_KID));
        idToken.put("typ", "id+jwt");
        line(out, "  a correctly signed token of another type, offered as an access token: "
                + outcome(validator, Issuer.issue(OLD_KEY.getPrivate(), idToken, Issuer.claims("ana", "accounts:read", START, 300))));
        line(out, "  the algorithm and the type are settings of the API; the header is only checked against them");
    }

    private static void act3Claims(StringBuilder out) {
        line(out, "THREE. Every claim that matters is checked.");
        SimClock clock = new SimClock(START);
        Validator validator = new Validator(Policy.bank(), bankKeys(), clock);
        String token = anaToken();
        line(out, "  at the moment of issue: " + outcome(validator, token));
        clock.advance(300 + 20);
        line(out, "  20 seconds after exp, inside the 30 seconds allowed for clock skew: " + outcome(validator, token));
        clock.advance(10);
        line(out, "  30 seconds after exp: " + outcome(validator, token));
        SimClock early = new SimClock(START);
        Map<String, Object> later = Issuer.claims("ana", "accounts:read", START, 300);
        later.put("nbf", START + 120);
        line(out, "  a token that starts in 2 minutes, presented now: "
                + outcome(new Validator(Policy.bank(), bankKeys(), early), Issuer.issue(OLD_KEY.getPrivate(), Issuer.header(OLD_KID), later)));
        Map<String, Object> otherIssuer = Issuer.claims("ana", "accounts:read", START, 300);
        otherIssuer.put("iss", "https://auth.partner.example");
        line(out, "  issued by another authorization server: "
                + outcome(new Validator(Policy.bank(), bankKeys(), early), Issuer.issue(OLD_KEY.getPrivate(), Issuer.header(OLD_KID), otherIssuer)));
        Map<String, Object> otherAudience = Issuer.claims("ana", "accounts:read", START, 300);
        otherAudience.put("aud", "loyalty-api");
        line(out, "  meant for the loyalty API, sent to the accounts API: "
                + outcome(new Validator(Policy.bank(), bankKeys(), early), Issuer.issue(OLD_KEY.getPrivate(), Issuer.header(OLD_KID), otherAudience)));
        line(out, "  with only the scope cards:read: "
                + outcome(new Validator(Policy.bank(), bankKeys(), early), Issuer.issue(OLD_KID, OLD_KEY.getPrivate(), "ana", "cards:read", START, 300)));
        line(out, "  a valid signature proves who wrote the token; only the claims say whether it is for this API, now");
    }

    private static void act4Keys(StringBuilder out) {
        line(out, "FOUR. Keys by id, from a set the API already holds.");
        SimClock clock = new SimClock(START + 10);
        KeySet keys = bankKeys().add(NEW_KID, NEW_KEY.getPublic());
        Validator validator = new Validator(Policy.bank(), keys, clock);
        line(out, "  during the changeover the API trusts " + keys.ids().size() + " keys: " + String.join(" and ", keys.ids()));
        String old = anaToken();
        String fresh = Issuer.issue(NEW_KID, NEW_KEY.getPrivate(), "ana", "accounts:read", START, 300);
        line(out, "  a token signed with the old key: " + outcome(validator, old));
        line(out, "  a token signed with the new key: " + outcome(validator, fresh));
        keys.remove(OLD_KID);
        line(out, "  after the old key is retired, the old token: " + outcome(validator, old));
        line(out, "  a token naming a key id the API has never seen: "
                + outcome(validator, Issuer.issue("attacker-key", INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300)));
        line(out, "  a token signed with another key but naming the bank's key id: "
                + outcome(validator, Issuer.issue(NEW_KID, INTRUDER_KEY.getPrivate(), "ana", "accounts:read", START, 300)));
        line(out, "  an unknown key id is refused; the API never fetches a key because a token asked it to");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a token cannot be taken back.");
        SimClock clock = new SimClock(START);
        Validator validator = new Validator(Policy.bank(), bankKeys(), clock);
        RefreshTokens refresh = new RefreshTokens();
        String access = anaToken();
        String handle = refresh.issue("ana");
        clock.advance(120);
        refresh.revokeAll("ana");
        line(out, "  ana's access is removed 2 minutes after her token was issued");
        clock.advance(120);
        line(out, "  2 minutes later her access token: " + outcome(validator, access) + "  <- STILL WORKS UNTIL IT EXPIRES");
        line(out, "  her refresh token, which the server looks up: " + (refresh.redeem(handle).isPresent() ? "accepted" : "refused (revoked)"));
        clock.advance(90);
        line(out, "  at 5 minutes and 30 seconds the access token: " + outcome(validator, access));
        line(out, "  so access tokens live for minutes, and a token asking for more than 15 is refused: "
                + outcome(new Validator(Policy.bank(), bankKeys(), new SimClock(START)),
                        Issuer.issue(OLD_KID, OLD_KEY.getPrivate(), "ana", "accounts:read", START, 3600)));
        line(out, "  where a browser should keep tokens is the tokens-in-the-browser project; in production, use a maintained library");
    }
}
