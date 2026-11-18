package com.jk.explore.security.oauthkeycloak;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the bank's OAuth 2.0 story in five numbered acts against a real Keycloak in Docker and a real resource server, all on the loopback
 * interface. Each step prints the request and the response in separate labelled sections. Codes, tokens, states, session ids and the port are
 * random, so they are printed as placeholders such as {@code <code>}; the visible flow uses the published RFC 7636 verifier.
 */
public class OAuthKeycloakDemo {

    static final String ACCOUNTS_READ = "openid accounts.read";
    static final String REDIRECT = OAuthClient.REDIRECT;
    static final String STATEMENTS_REDIRECT = "https://statements.example/callback";
    static final String RFC_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String RFC_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    /** The realm's code lifetime in seconds (accessCodeLifespan in infra/bank-realm.json): long enough never to expire mid-demo. */
    static final long CODE_LIFETIME = 10;

    /** A redirect check that only looks at the start of the address: the mistake exact matching prevents. */
    static final String PREFIX = "https://budget.example";

    /** Output lines stay within this width, so each fits on one line of a slide. */
    static final int WIDTH = 70;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (KeycloakLab keycloak = KeycloakLab.start(); Running api = Running.start(keycloak)) {
            OAuthClient app = new OAuthClient(keycloak.base(), keycloak.issuer());
            act1Flow(out, keycloak, app, api);
            act2Pkce(out, app);
            act3RedirectsStateAndCodes(out, app, api);
            act4ScopesAndRefresh(out, app, api);
            act5Practice(out, app);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Signs Ana in with a fresh verifier and state and returns the redirect, with the verifier for the exchange. */
    record Started(OAuthClient.Outcome outcome, String verifier, String state) {

        String code() {
            return OAuthClient.codeIfStateMatches(outcome, state);
        }
    }

    static Started start(OAuthClient app, String user, String scope) throws IOException, InterruptedException {
        return start(app, user, scope, OAuthClient.newVerifier());
    }

    static Started start(OAuthClient app, String user, String scope, String verifier) throws IOException, InterruptedException {
        String state = OAuthClient.newState();
        return new Started(app.signIn(OAuthClient.authorizationRequest("budget-app", REDIRECT, scope, state, OAuthClient.challenge(verifier)), user, "demo-password"), verifier, state);
    }

    static Map<String, String> exchange(String code, String verifier) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", "budget-app");
        form.put("code", code);
        form.put("redirect_uri", REDIRECT);
        if (verifier != null) {
            form.put("code_verifier", verifier);
        }
        return form;
    }

    static Map<String, Object> claims(String jwt) {
        return JSON.readValue(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), new TypeReference<Map<String, Object>>() {
        });
    }

    /** A random value replaced by what it is, and Keycloak's random port by {@code <keycloak>}, so the output is the same on every run. */
    static String shown(String name, String value, String keycloakBase) {
        return switch (name) {
            case "state", "code" -> "<" + name + ">";
            case "session_state" -> "<session>";
            case "access_token", "refresh_token", "id_token" -> "<" + name.replace('_', ' ') + ">";
            default -> value.replace(keycloakBase, "<keycloak>");
        };
    }

    /** An address printed one parameter per line: the address, then ?first and &rest. */
    static void url(StringBuilder out, String indent, String head, String url, String keycloakBase) {
        int q = url.indexOf('?');
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : url.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            params.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        params(out, indent, head + url.substring(0, q).replace(keycloakBase, "<keycloak>"), "?", "&", params, keycloakBase);
    }

    private static void params(StringBuilder out, String indent, String head, String first, String rest, Map<String, String> params, String keycloakBase) {
        line(out, indent + head);
        String sep = first;
        for (Map.Entry<String, String> e : params.entrySet()) {
            line(out, indent + "  " + sep + e.getKey() + "=" + shown(e.getKey(), e.getValue(), keycloakBase));
            sep = rest;
        }
    }

    /** A refusal, given as {@code error: description}: the error code, then its reason wrapped to fit. A null error means it worked. */
    static void refused(StringBuilder out, String indent, String error) {
        if (error == null) {
            line(out, indent + "-> tokens issued");
            return;
        }
        int colon = error.indexOf(": ");
        line(out, indent + "-> refused: " + error.substring(0, colon));
        String pad = indent + "   ";
        StringBuilder row = new StringBuilder();
        for (String word : error.substring(colon + 2).split(" ")) {
            if (!row.isEmpty() && pad.length() + row.length() + 1 + word.length() > WIDTH) {
                line(out, pad + row);
                row.setLength(0);
            }
            row.append(row.isEmpty() ? "" : " ").append(word);
        }
        line(out, pad + row);
    }

    private static String error(OAuthClient.TokenReply reply) {
        return reply.ok() ? null : reply.error();
    }

    private static void act1Flow(StringBuilder out, KeycloakLab keycloak, OAuthClient app, Running api) throws IOException, InterruptedException {
        String base = keycloak.base();
        line(out, "ONE. The authorization code flow, step by step.");
        Started s = start(app, "ana", ACCOUNTS_READ, RFC_VERIFIER);
        Map<String, String> request = OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, s.state(), OAuthClient.challenge(RFC_VERIFIER));
        line(out, "  STEP 1, THE REQUEST (the app sends Ana's browser to Keycloak):");
        params(out, "    ", "GET <keycloak>/realms/bank/protocol/openid-connect/auth", "?", "&", request, base);
        line(out, "  STEP 2, AT KEYCLOAK (never at the app):");
        line(out, "    Ana signs in with her bank password");
        line(out, "    the consent page asks for \"Read your accounts\": " + s.outcome().consentShown());
        line(out, "    Ana agrees");
        line(out, "  STEP 3, THE RESPONSE (Keycloak sends the browser back):");
        url(out, "    ", "302 Location: ", s.outcome().location(), base);
        line(out, "    the state comes back unchanged: " + s.state().equals(s.outcome().parameters().get("state")));
        line(out, "    iss names the bank realm: " + keycloak.issuer().equals(s.outcome().parameters().get("iss")));
        String code = s.code();
        Map<String, String> form = exchange(code, s.verifier());
        line(out, "  STEP 4, THE REQUEST (the app calls Keycloak directly):");
        params(out, "    ", "POST <keycloak>/realms/bank/protocol/openid-connect/token", "", "", form, base);
        OAuthClient.TokenReply tokens = app.token(form);
        line(out, "  STEP 5, THE RESPONSE (to the app, not to the browser):");
        Map<String, Object> c = claims(tokens.string("access_token"));
        // expires_in counts from the moment Keycloak answers (299 or 300); the token's own exp - iat is always the lifetime
        long lifetime = ((Number) c.get("exp")).longValue() - ((Number) c.get("iat")).longValue();
        line(out, "    access_token=<access token>  lives " + lifetime + " s (exp - iat)");
        line(out, "    refresh_token=" + shown("refresh_token", tokens.string("refresh_token"), base));
        line(out, "    id_token=" + shown("id_token", tokens.string("id_token"), base));
        line(out, "    scope=" + tokens.string("scope"));
        line(out, "  INSIDE THE ACCESS TOKEN (a JWT signed by Keycloak):");
        line(out, "    aud=" + c.get("aud") + "  azp=" + c.get("azp"));
        line(out, "    iss=" + String.valueOf(c.get("iss")).replace(base, "<keycloak>"));
        line(out, "    sub=" + c.get("sub") + (AccountsController.ANA.equals(c.get("sub")) ? " (Ana)" : ""));
        line(out, "    scope=" + c.get("scope"));
        line(out, "    no password, no e-mail address: " + (!c.containsKey("email") && !c.containsKey("password")));
        Running.Reply accounts = api.get("/accounts", tokens.string("access_token"));
        Map<String, Object> body = JSON.readValue(accounts.body(), new TypeReference<Map<String, Object>>() {
        });
        line(out, "  STEP 6, THE APP CALLS THE BANK'S API:");
        line(out, "    GET /accounts  Authorization: Bearer <access token>");
        line(out, "    -> " + accounts.status() + " " + body.get("owner") + ": " + body.get("accounts") + " accounts");
        line(out, "    GET /accounts with no token");
        line(out, "    -> " + api.get("/accounts", null).status());
        line(out, "  WHO SAW WHAT:");
        line(out, "    the app never saw Ana's password");
        line(out, "    the browser never saw the access token");
    }

    private static void act2Pkce(StringBuilder out, OAuthClient app) throws IOException, InterruptedException {
        line(out, "TWO. PKCE: a code is useless without the verifier.");
        line(out, "  THE VERIFIER (made by the app for one sign-in, kept secret):");
        line(out, "    " + RFC_VERIFIER);
        line(out, "    " + RFC_VERIFIER.length() + " characters from A-Z a-z 0-9 - . _ ~ (the RFC 7636 example)");
        byte[] ascii = ChallengeSteps.ascii(RFC_VERIFIER);
        byte[] hash = ChallengeSteps.sha256(ascii);
        String challenge = ChallengeSteps.base64Url(hash);
        line(out, "  HOW THE S256 CHALLENGE IS MADE:");
        line(out, "    1. the " + RFC_VERIFIER.length() + " characters as " + ascii.length + " ASCII bytes:");
        line(out, "       " + ChallengeSteps.hex(ascii, 0, 16) + " ...");
        line(out, "    2. SHA-256 of those bytes, always " + hash.length + " bytes:");
        line(out, "       " + ChallengeSteps.hex(hash, 0, 16));
        line(out, "       " + ChallengeSteps.hex(hash, 16, 32));
        line(out, "    3. base64url: every 3 bytes become 4 characters of 6 bits");
        line(out, "       " + ChallengeSteps.firstGroup(hash));
        line(out, "    4. " + hash.length + " bytes become " + challenge.length() + " characters, with no = padding:");
        line(out, "       " + challenge);
        line(out, "    the same as the challenge RFC 7636 publishes: " + challenge.equals(RFC_CHALLENGE));
        line(out, "    the same as OAuthClient.challenge, with Java's Base64: " + challenge.equals(OAuthClient.challenge(RFC_VERIFIER)));
        Map<String, String> request = OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), challenge);
        line(out, "  WHAT TRAVELS THROUGH THE BROWSER (step 1):");
        line(out, "    the challenge: " + request.containsValue(RFC_CHALLENGE));
        line(out, "    the verifier: " + request.containsValue(RFC_VERIFIER));

        line(out, "  THE LEAK (a thief redeems a copied code before the app):");
        Started leaked = start(app, "ana", ACCOUNTS_READ);
        String code = leaked.code();
        line(out, "    the thief, with no verifier");
        refused(out, "      ", error(app.token(exchange(code, null))));
        line(out, "    then the budgeting app, with the real verifier");
        refused(out, "      ", error(app.token(exchange(code, leaked.verifier()))));
        line(out, "  a failed try burns the code: the leak is useless, Ana signs in again");
        line(out, "  A NEW CODE, AND A VERIFIER THE THIEF MADE UP (43 x's):");
        Started madeUp = start(app, "ana", ACCOUNTS_READ);
        refused(out, "    ", error(app.token(exchange(madeUp.code(), "x".repeat(43)))));
        line(out, "  A NEW CODE, AND THE REAL VERIFIER:");
        Started good = start(app, "ana", ACCOUNTS_READ);
        refused(out, "    ", error(app.token(exchange(good.code(), good.verifier()))));

        line(out, "  WEAKER REQUESTS, REFUSED BY KEYCLOAK:");
        Map<String, String> plain = OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), null);
        plain.put("code_challenge", RFC_VERIFIER);
        plain.put("code_challenge_method", "plain");
        line(out, "    code_challenge_method=plain (the verifier itself travels)");
        refused(out, "      ", app.signIn(plain, "ana", "demo-password").error());
        line(out, "    no code_challenge at all");
        refused(out, "      ", app.signIn(OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), null), "ana", "demo-password").error());
    }

    private static void act3RedirectsStateAndCodes(StringBuilder out, OAuthClient app, Running api) throws IOException, InterruptedException {
        line(out, "THREE. Exact redirect addresses, state, and codes that work once.");
        line(out, "  THE REGISTERED ADDRESS (Keycloak compares it exactly):");
        line(out, "    " + REDIRECT);
        line(out, String.format("  %-45s%-9s%s", "THE ADDRESS IN THE REQUEST:", "EXACT", "PREFIX"));
        int sentAway = 0;
        TreeSet<String> pages = new TreeSet<>();
        for (String redirect : List.of(REDIRECT, REDIRECT + "/../steal", "https://budget.example.evil.test/callback", REDIRECT + "?next=evil",
                "https://BUDGET.example/callback", REDIRECT + "/")) {
            Map<String, String> request = OAuthClient.authorizationRequest("budget-app", redirect, ACCOUNTS_READ, OAuthClient.newState(),
                    OAuthClient.challenge(OAuthClient.newVerifier()));
            OAuthClient.Outcome o = app.signIn(request, "ana", "demo-password");
            if (!o.redirected()) {
                pages.add(o.errorPage());
            }
            boolean prefix = redirect.startsWith(PREFIX);
            if (prefix && !redirect.equals(REDIRECT)) {
                sentAway++;
            }
            line(out, String.format("    %-43s%-9s%s", redirect, o.redirected() ? "code" : "refused", prefix ? "code" : "refused"));
        }
        line(out, "  each refusal is a page at Keycloak, with no redirect:");
        for (String page : pages) {
            line(out, "    " + page);
        }
        line(out, "  a prefix check on " + PREFIX + " sends the code away " + sentAway + " times");

        line(out, "  THE ATTACK (login forgery):");
        line(out, "    Ana's browser opens a callback link with the attacker's code");
        line(out, "    and state, to sign Ana in to the attacker's account");
        Started attacker = start(app, "ben", ACCOUNTS_READ);
        line(out, "  THE APP'S CHECK (the state against Ana's browser session):");
        try {
            OAuthClient.codeIfStateMatches(attacker.outcome(), OAuthClient.newState());
            line(out, "    -> accepted");
        } catch (IllegalStateException e) {
            refused(out, "    ", e.getMessage());
        }

        Started once = start(app, "ana", ACCOUNTS_READ);
        String code = once.code();
        OAuthClient.TokenReply first = app.token(exchange(code, once.verifier()));
        line(out, "  ONE CODE, REDEEMED TWICE:");
        line(out, "    first: tokens issued, GET /accounts -> " + api.get("/accounts", first.string("access_token")).status());
        line(out, "    second, by whoever copied the code:");
        refused(out, "      ", error(app.token(exchange(code, once.verifier()))));
        line(out, "    the first access token now:");
        line(out, "      at Keycloak (userinfo) -> " + app.userinfo(first.string("access_token")) + "  <- THE SESSION IS ENDED");
        line(out, "      at the API (own signature check) -> " + api.get("/accounts", first.string("access_token")).status()
                + "  <- UNTIL IT EXPIRES");

        Started late = start(app, "ana", ACCOUNTS_READ);
        Thread.sleep((CODE_LIFETIME + 1) * 1000L);
        line(out, "  ONE CODE, REDEEMED " + (CODE_LIFETIME + 1) + " SECONDS LATER (this realm gives codes " + CODE_LIFETIME + "):");
        line(out, "    by the budgeting app, with the real verifier");
        refused(out, "      ", error(app.token(exchange(late.code(), late.verifier()))));
    }

    private static void act4ScopesAndRefresh(StringBuilder out, OAuthClient app, Running api) throws IOException, InterruptedException {
        line(out, "FOUR. Scopes and refresh tokens.");
        Started s = start(app, "ana", ACCOUNTS_READ);
        OAuthClient.TokenReply tokens = app.token(exchange(s.code(), s.verifier()));
        String access = tokens.string("access_token");
        line(out, "  THE SCOPE (asked for, agreed to, carried by the token):");
        line(out, "    " + claims(access).get("scope"));
        line(out, "  THE API CALLS WITH THAT TOKEN:");
        line(out, "    GET /accounts -> " + api.get("/accounts", access).status());
        line(out, "    POST /payments -> " + api.post("/payments", access).text());
        Map<String, String> greedy = OAuthClient.authorizationRequest("budget-app", REDIRECT, "openid accounts.read payments.write", OAuthClient.newState(),
                OAuthClient.challenge(OAuthClient.newVerifier()));
        line(out, "  ASKING FOR payments.write TOO (never registered):");
        refused(out, "    ", app.signIn(greedy, "ana", "demo-password").error());

        Map<String, String> refresh = refreshForm(tokens.string("refresh_token"));
        OAuthClient.TokenReply renewed = app.token(refresh);
        line(out, "  THE REFRESH (access token A1 ran out; the app sends R1):");
        line(out, "    R1 -> a new access token A2 and a new refresh token R2");
        line(out, "    R2 differs from R1, and A2 from A1: " + (renewed.ok() && !renewed.string("refresh_token").equals(tokens.string("refresh_token"))
                && !renewed.string("access_token").equals(access)));
        line(out, "  THE THEFT (a thief copied R1 and replays it):");
        line(out, "    R1 again");
        refused(out, "      ", error(app.token(refresh)));
        line(out, "    R2, held by the real app");
        refused(out, "      ", error(app.token(refreshForm(renewed.string("refresh_token")))));
        line(out, "    A2 at Keycloak (userinfo) -> " + app.userinfo(renewed.string("access_token")) + "  <- THE SESSION IS ENDED");

        Started t = start(app, "ana", ACCOUNTS_READ);
        String fresh = app.token(exchange(t.code(), t.verifier())).string("access_token");
        Duration age = Duration.ofSeconds(331);
        api.clock().advance(age);
        line(out, "  AN ACCESS TOKEN " + age.toSeconds() + " SECONDS OLD (300, plus 30 for clock skew):");
        line(out, "    GET /accounts -> " + api.get("/accounts", fresh).status());
    }

    private static Map<String, String> refreshForm(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", "budget-app");
        form.put("refresh_token", refreshToken);
        return form;
    }

    private static void act5Practice(StringBuilder out, OAuthClient app) throws IOException, InterruptedException {
        line(out, "FIVE. Current practice, and the bill.");
        Map<String, String> implicit = OAuthClient.authorizationRequest("budget-app", REDIRECT, "openid", OAuthClient.newState(), null);
        implicit.put("response_type", "token");
        line(out, "  THE IMPLICIT FLOW (response_type=token: a token in the address):");
        refused(out, "    ", app.signIn(implicit, "ana", "demo-password").error());
        line(out, "  THE PASSWORD GRANT (the app would hold Ana's password):");
        Map<String, String> password = new LinkedHashMap<>();
        password.put("grant_type", "password");
        password.put("client_id", "budget-app");
        password.put("username", "ana");
        password.put("password", "demo-password");
        refused(out, "    ", error(app.token(password)));
        line(out, "  both are removed by RFC 9700 and by OAuth 2.1");

        String verifier = OAuthClient.newVerifier();
        Map<String, String> request = OAuthClient.authorizationRequest("statements-service", STATEMENTS_REDIRECT, ACCOUNTS_READ, OAuthClient.newState(),
                OAuthClient.challenge(verifier));
        String code = OAuthClient.codeIfStateMatches(app.signIn(request, "ana", "demo-password"), request.get("state"));
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", "statements-service");
        form.put("code", code);
        form.put("redirect_uri", STATEMENTS_REDIRECT);
        form.put("client_secret", "wrong");
        line(out, "  A CONFIDENTIAL CLIENT (statements-service, on a server):");
        line(out, "    a wrong client secret");
        refused(out, "      ", error(app.token(form)));
        form.put("client_secret", "demo-statements-secret");
        form.put("code", "made-up");
        form.put("code_verifier", verifier);
        line(out, "    the right secret, but a code it was never given");
        refused(out, "      ", error(app.token(form)));
        form.put("code", code);
        form.remove("code_verifier");
        line(out, "    the right secret and a real code, but no verifier");
        refused(out, "      ", error(app.token(form)));
        line(out, "  a secret does not replace PKCE; a phone app has no secret at all");
        line(out, "  WHAT THE API CHECKS ON EVERY TOKEN (it keeps no session):");
        line(out, "    Keycloak's signing key, the issuer, the audience " + ApiSecurity.AUDIENCE + ",");
        line(out, "    the dates and the scope");
        line(out, "  in production: TLS, backups, key rotation, login monitoring");
    }
}
