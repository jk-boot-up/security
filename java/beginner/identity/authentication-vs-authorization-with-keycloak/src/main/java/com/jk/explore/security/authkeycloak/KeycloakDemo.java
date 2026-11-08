package com.jk.explore.security.authkeycloak;

import com.nimbusds.jose.util.JSONObjectUtils;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Runs the online store story in six numbered acts against a real Spring Boot resource server on the loopback interface.
 * The tokens come from a stand-in for Keycloak, so the demo needs no container; the integration tests use the real Keycloak.
 * Each act prints what goes in (the requests, or the state before) and what comes out (the responses, or the state after)
 * in separate sections.
 */
public class KeycloakDemo {

    /** The claims the stand-in login server puts in a token, in the order they are explained. */
    private static final List<String> CLAIM_ORDER = List.of("iss", "sub", "aud", "azp", "preferred_username", "realm_access", "iat", "exp", "jti");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static JwtDecoder decoder(Issuer issuer, SimClock clock, List<OAuth2TokenValidator<Jwt>> extra) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(issuer.publicKey()).build();
        decoder.setJwtValidator(Validators.all(Issuer.REALM_ISSUER, Issuer.AUDIENCE, clock, extra));
        return decoder;
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys are random, so only facts that do not depend on them are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        SimClock clock = new SimClock(Instant.parse("2026-01-01T09:00:00Z"));
        Issuer issuer = new Issuer(Issuer.REALM_ISSUER, clock);
        try (Running api = Running.start(decoder(issuer, clock, List.of()))) {
            act1Tokens(out, api, issuer, clock);
            act2TokenAndSignature(out, api, issuer);
            act3Roles(out, api, issuer);
            act4IssuerAndAudience(out, api, issuer);
        }
        act5StaleRoles(out, issuer, clock);
        act6Revocation(out, issuer, clock);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** "200  ana [ROLE_CUSTOMER]": the status code, and the body when the answer is 200. */
    private static String answer(Running api, String path, String token) throws IOException, InterruptedException {
        HttpResponse<String> response = api.get(path, token);
        return response.statusCode() == 200 ? "200  " + response.body() : String.valueOf(response.statusCode());
    }

    private static void act1Tokens(StringBuilder out, Running api, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "ONE. No login in the shop, only tokens.");
        String forged = issuer.forge("ana", List.of("customer"));
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        line(out, "  THE REQUESTS (GET /me, each with a different token):");
        line(out, "    1  no token at all");
        line(out, "    2  the text not-a-token as the token");
        line(out, "    3  a token signed with another key");
        line(out, "    4  a token the login server signed, good for 300 s");
        line(out, "    5  the same token, 301 seconds later");
        String[] answers = new String[5];
        answers[0] = answer(api, "/me", null);
        answers[1] = answer(api, "/me", "not-a-token");
        answers[2] = answer(api, "/me", forged);
        answers[3] = answer(api, "/me", token);
        clock.advanceSeconds(301);
        answers[4] = answer(api, "/me", token);
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + answers[0] + "  nobody is logged in");
        line(out, "    2  " + answers[1] + "  not a token");
        line(out, "    3  " + answers[2] + "  the signature does not match");
        line(out, "    4  " + answers[3]);
        line(out, "    5  " + answers[4] + "  expired  <- NO CALL TO THE LOGIN SERVER");
    }

    /** The token as sent and its three parts decoded, then the signature check done by hand, then a tampered copy. */
    private static void act2TokenAndSignature(StringBuilder out, Running api, Issuer issuer) throws IOException, InterruptedException {
        line(out, "TWO. Inside a token, and its signature.");
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        String[] parts = token.split("\\.");
        line(out, "  BEFORE DECODING (the token as sent, " + token.length() + " characters):");
        line(out, "    " + token.substring(0, 44) + "...");
        line(out, "  AFTER DECODING (base64url, split at the two dots):");
        line(out, "    header     " + decode(parts[0]));
        Map<String, Object> claims = claims(parts[1]);
        String first = "    claims     ";
        for (String name : CLAIM_ORDER) {                   // shown in a fixed order, easier to read
            line(out, first + String.format("%-12s ", name) + shown(name, claims.get(name)));
            first = "               ";
        }
        line(out, "    signature  " + Base64.getUrlDecoder().decode(parts[2]).length + " bytes, made with the private key");
        signatureSteps(out, api, issuer, token);
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> claims(String part) {
        try {
            return JSONObjectUtils.parse(decode(part));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String shown(String name, Object value) {
        if (value instanceof Map<?, ?> map) {
            return JSONObjectUtils.toJSONString((Map<String, ?>) map);
        }
        if (name.equals("iat") || name.equals("exp")) {
            return value + " = " + Instant.ofEpochSecond(((Number) value).longValue()).toString().replace("T", " ");
        }
        return String.valueOf(value);
    }

    private static void signatureSteps(StringBuilder out, Running api, Issuer issuer, String token) throws IOException, InterruptedException {
        SignatureSteps.Steps steps = SignatureSteps.check(token, issuer.publicKey());
        line(out, "  STEP 1. THE SIGNED TEXT = header + \".\" + claims, as sent:");
        line(out, "    " + steps.signedText().substring(0, 44) + "...");
        line(out, "  STEP 2. ITS HASH = SHA-256(signed text), 32 bytes:");
        line(out, "    hash     " + SignatureSteps.hex(steps.hash(), 0, 16));
        line(out, "             " + SignatureSteps.hex(steps.hash(), 16, 32));
        line(out, "  STEP 3. OPEN THE SIGNATURE: signature ^ e mod n (public key)");
        line(out, "    e = " + issuer.publicKey().getPublicExponent() + ", n has " + issuer.publicKey().getModulus().bitLength() + " bits, the result has "
                + steps.opened().length + " bytes:");
        line(out, "    0001, then " + steps.paddingBytes() + " bytes of ff, then 00");
        line(out, "    " + SignatureSteps.hex(SignatureSteps.SHA256_PREFIX, 0, 19) + " = \"SHA-256\"");
        line(out, "    last 32  " + SignatureSteps.hex(steps.hashInSignature(), 0, 16));
        line(out, "             " + SignatureSteps.hex(steps.hashInSignature(), 16, 32));
        line(out, "  STEP 4. COMPARE the two hashes: " + steps.matches());
        String changed = tamper(token, "\"customer\"", "\"admin\"");
        SignatureSteps.Steps after = SignatureSteps.check(changed, issuer.publicKey());
        line(out, "  BEFORE TAMPERING (roles [customer]): GET /me " + answer(api, "/me", token).substring(0, 3));
        line(out, "  AFTER TAMPERING (customer -> admin, same signature):");
        line(out, "    hash     " + SignatureSteps.hex(after.hash(), 0, 16));
        line(out, "             " + SignatureSteps.hex(after.hash(), 16, 32));
        line(out, "    in sig   " + SignatureSteps.hex(after.hashInSignature(), 0, 16));
        line(out, "    compare  " + after.matches() + ": GET /me " + answer(api, "/me", changed) + "  <- BROKEN");
    }

    /** The token with one piece of text in its claims replaced, and the old signature kept. */
    static String tamper(String token, String from, String to) {
        String[] parts = token.split("\\.");
        String claims = decode(parts[1]).replace(from, to);
        return parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
    }

    private static void act3Roles(StringBuilder out, Running api, Issuer issuer) throws IOException, InterruptedException {
        line(out, "THREE. Who is it, and what may they do.");
        String ana = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        String sam = issuer.mint("sam", List.of("support"), 300, Issuer.AUDIENCE);
        String root = issuer.mint("root", List.of("admin"), 300, Issuer.AUDIENCE);
        line(out, "  IN THE TOKEN (the claim realm_access.roles):");
        line(out, "    ana [customer]   sam [support]   root [admin]");
        line(out, "  IN THE SHOP (authorities: ROLE_ + the role in capitals):");
        line(out, "    " + api.get("/me", ana).body() + "   " + api.get("/me", sam).body() + "   " + api.get("/me", root).body());
        line(out, "  THE REQUESTS:");
        line(out, "    1  ana   GET /orders/A-1     her own order");
        line(out, "    2  ana   GET /orders/B-2     Ben's order");
        line(out, "    3  sam   GET /orders/B-2     support may read any order");
        line(out, "    4  ana   GET /admin/report");
        line(out, "    5  sam   GET /admin/report");
        line(out, "    6  root  GET /admin/report");
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + answer(api, "/orders/A-1", ana));
        line(out, "    2  " + answer(api, "/orders/B-2", ana) + "  <- LOGGED IN, NOT ALLOWED");
        line(out, "    3  " + answer(api, "/orders/B-2", sam));
        line(out, "    4  " + answer(api, "/admin/report", ana));
        line(out, "    5  " + answer(api, "/admin/report", sam));
        line(out, "    6  " + answer(api, "/admin/report", root));
        line(out, "  401: who are you?  403: I know you, and no");
    }

    private static void act4IssuerAndAudience(StringBuilder out, Running api, Issuer issuer) throws IOException, InterruptedException {
        line(out, "FOUR. A real signature is not enough.");
        String otherIssuer = "http://login.elsewhere.example/realms/other";
        String other = issuer.mintWithIssuer("ana", List.of("customer"), 300, Issuer.AUDIENCE, otherIssuer);
        String wrongAudience = issuer.mint("ana", List.of("customer"), 300, "another-api");
        line(out, "  THE REQUESTS (GET /me, both signed with the right key):");
        line(out, "    1  iss " + otherIssuer);
        line(out, "    2  aud another-api");
        line(out, "  THE RESPONSES (wanted: .../realms/shop and shop-api):");
        line(out, "    1  " + answer(api, "/me", other) + "  another issuer");
        line(out, "    2  " + answer(api, "/me", wrongAudience) + "  another API  <- ELSE ANY API'S TOKEN WOULD WORK");
        line(out, "  checked: signature, expiry, issuer, audience");
    }

    private static void act5StaleRoles(StringBuilder out, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "FIVE. The roles in a token are a copy.");
        Map<String, List<String>> directory = new HashMap<>();
        directory.put("root", List.of("admin"));
        try (Running api = Running.start(decoder(issuer, clock, List.of()))) {
            String token = issuer.mint("root", directory.get("root"), 300, Issuer.AUDIENCE);
            line(out, "  BEFORE THE DEMOTION (root's token says [admin]):");
            line(out, "    GET /admin/report  " + answer(api, "/admin/report", token));
            directory.put("root", List.of("customer"));
            line(out, "  root is demoted to customer in the login server's directory");
            line(out, "  AFTER THE DEMOTION:");
            line(out, "    the same token:     GET /admin/report  " + answer(api, "/admin/report", token).substring(0, 3) + "  <- ONLY A COPY");
            clock.advanceSeconds(301);
            line(out, "    301 seconds later:  GET /admin/report  " + answer(api, "/admin/report", token) + "  expired");
            String fresh = issuer.mint("root", directory.get("root"), 300, Issuer.AUDIENCE);
            line(out, "    a new token says " + directory.get("root") + ":");
            line(out, "                        GET /admin/report  " + answer(api, "/admin/report", fresh));
            line(out, "                        GET /me            " + answer(api, "/me", fresh).substring(0, 3));
            line(out, "  the stale window is at most the token's lifetime: 300 s");
        }
    }

    private static void act6Revocation(StringBuilder out, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "SIX. Ending a token early, and the bill.");
        Revocations revocations = new Revocations();
        try (Running api = Running.start(decoder(issuer, clock, List.of(revocations)))) {
            String token = issuer.mint("root", List.of("admin"), 300, Issuer.AUDIENCE);
            String id = issuer.lastTokenId();
            line(out, "  BEFORE REVOKING (root's admin token, jti " + id + "):");
            line(out, "    GET /admin/report  " + answer(api, "/admin/report", token));
            line(out, "    GET /admin/report  " + answer(api, "/admin/report", token));
            revocations.revoke(id);
            line(out, "  " + id + " is put on the revocation list");
            line(out, "  AFTER REVOKING:");
            line(out, "    GET /admin/report  " + answer(api, "/admin/report", token) + "  <- ENDED AT ONCE");
            line(out, "  THE BILL:");
            line(out, "    the list was read " + revocations.lookups() + " times for 3 requests: every time");
        }
        line(out, "    without a list: no call, but the token works until expiry");
        line(out, "    refresh tokens: long sessions with short access tokens");
        line(out, "    the login server: one more system to run, patch, back up");
    }
}
