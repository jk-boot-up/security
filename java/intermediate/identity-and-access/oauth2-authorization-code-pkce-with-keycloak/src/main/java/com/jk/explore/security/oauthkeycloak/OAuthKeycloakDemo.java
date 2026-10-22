package com.jk.explore.security.oauthkeycloak;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Runs the bank's OAuth 2.0 story in five numbered acts against a real Keycloak in Docker and a real resource server, all on the loopback interface. */
public class OAuthKeycloakDemo {

    static final String ACCOUNTS_READ = "openid accounts.read";
    static final String REDIRECT = OAuthClient.REDIRECT;
    static final String STATEMENTS_REDIRECT = "https://statements.example/callback";
    static final String RFC_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String RFC_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Codes, tokens, ports and session ids are never printed. */
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
        String verifier = OAuthClient.newVerifier();
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

    private static String refused(OAuthClient.TokenReply r) {
        return r.ok() ? "tokens issued" : "refused (" + r.error() + ")";
    }

    private static void act1Flow(StringBuilder out, KeycloakLab keycloak, OAuthClient app, Running api) throws IOException, InterruptedException {
        line(out, "ONE. The authorization code flow, step by step.");
        line(out, "  1. the budgeting app sends Ana's browser to the bank's authorization server:");
        line(out, "     GET <keycloak>/realms/bank/protocol/openid-connect/auth?response_type=code&client_id=budget-app&redirect_uri=" + REDIRECT
                + "&scope=openid accounts.read&state=<state>&code_challenge=<code challenge>&code_challenge_method=S256");
        Started s = start(app, "ana", ACCOUNTS_READ);
        line(out, "  2. Ana signs in at Keycloak, never at the app, and is asked: budget-app wants to \"Read your accounts\": the consent page said so: " + s.outcome().consentShown() + ". Ana agrees");
        String back = s.outcome().location().replace(keycloak.base(), "<keycloak>");
        line(out, "  3. Keycloak sends the browser back: 302 " + REDIRECT + "?state=<state>&session_state=<session>&iss=<keycloak>/realms/bank&code=<code>");
        line(out, "     the address starts with the registered redirect address: " + back.startsWith(REDIRECT + "?")
                + ", the state comes back unchanged: " + s.state().equals(s.outcome().parameters().get("state"))
                + ", it names the issuer: " + keycloak.issuer().equals(s.outcome().parameters().get("iss")));
        String code = s.code();
        line(out, "  4. the app checks the state, then calls Keycloak directly, not through the browser:");
        line(out, "     POST <keycloak>/realms/bank/protocol/openid-connect/token grant_type=authorization_code&code=<code>&redirect_uri=" + REDIRECT + "&client_id=budget-app&code_verifier=<code verifier>");
        OAuthClient.TokenReply tokens = app.token(exchange(code, s.verifier()));
        line(out, "  5. Keycloak answers with an access token for " + tokens.string("expires_in") + " seconds, scope \"" + tokens.string("scope") + "\", a refresh token and an ID token: "
                + (tokens.string("refresh_token") != null && tokens.string("id_token") != null));
        Running.Reply accounts = api.get("/accounts", tokens.string("access_token"));
        line(out, "  6. the app reads Ana's accounts: GET /accounts with the access token: " + accounts.status() + ", " + JSON.readValue(accounts.body(), new TypeReference<Map<String, Object>>() {
        }).get("owner") + ": " + JSON.readValue(accounts.body(), new TypeReference<Map<String, Object>>() {
        }).get("accounts") + " accounts");
        Map<String, Object> c = claims(tokens.string("access_token"));
        line(out, "  the access token is for " + c.get("aud") + ", asked for by " + c.get("azp") + ", issued by the bank realm: " + keycloak.issuer().equals(c.get("iss"))
                + ", for Ana's id: " + AccountsController.ANA.equals(c.get("sub")) + ", and carries no password and no e-mail address: " + (!c.containsKey("email") && !c.containsKey("password")));
        line(out, "  the app never saw Ana's password, and the browser never saw the access token");
        line(out, "  without a token: GET /accounts: " + api.get("/accounts", null).status());
    }

    private static void act2Pkce(StringBuilder out, OAuthClient app) throws IOException, InterruptedException {
        line(out, "TWO. PKCE: a code is useless without the verifier.");
        line(out, "  the RFC 7636 example verifier " + RFC_VERIFIER);
        line(out, "  gives the S256 challenge " + OAuthClient.challenge(RFC_VERIFIER) + ": the published value: " + OAuthClient.challenge(RFC_VERIFIER).equals(RFC_CHALLENGE));
        String verifier = OAuthClient.newVerifier();
        Map<String, String> request = OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), OAuthClient.challenge(verifier));
        line(out, "  only the challenge travels through the browser: the request carries it: " + request.containsKey("code_challenge") + ", and the verifier: "
                + request.containsValue(verifier));
        line(out, "  the code leaks, say through a log or a browser history, and another app redeems it:");
        Started leaked = start(app, "ana", ACCOUNTS_READ);
        String code = leaked.code();
        line(out, "    with no verifier: " + refused(app.token(exchange(code, null))));
        line(out, "    then the budgeting app, with the real verifier: " + refused(app.token(exchange(code, leaked.verifier()))) + "  <- A FAILED TRY BURNS THE CODE: THE LEAK IS USELESS, AND ANA SIGNS IN AGAIN");
        Started madeUp = start(app, "ana", ACCOUNTS_READ);
        line(out, "  a new code and a verifier the thief made up, as long as a real one: " + refused(app.token(exchange(madeUp.code(), "x".repeat(43)))));
        Started good = start(app, "ana", ACCOUNTS_READ);
        line(out, "  a new code, redeemed by the budgeting app with the real verifier: " + refused(app.token(exchange(good.code(), good.verifier()))));
        Map<String, String> plain = OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), null);
        plain.put("code_challenge", verifier);
        plain.put("code_challenge_method", "plain");
        line(out, "  a request with code_challenge_method=plain, which sends the verifier itself: refused (" + app.signIn(plain, "ana", "demo-password").error() + ")");
        line(out, "  a request with no challenge at all: refused (" + app.signIn(OAuthClient.authorizationRequest("budget-app", REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), null), "ana", "demo-password").error() + ")");
    }

    private static void act3RedirectsStateAndCodes(StringBuilder out, OAuthClient app, Running api) throws IOException, InterruptedException {
        line(out, "THREE. Exact redirect addresses, state, and codes that work once.");
        line(out, "  the budgeting app registered exactly " + REDIRECT);
        for (String redirect : List.of(REDIRECT, REDIRECT + "/../steal", "https://budget.example.evil.test/callback", REDIRECT + "?next=evil", "https://BUDGET.example/callback", REDIRECT + "/")) {
            Map<String, String> request = OAuthClient.authorizationRequest("budget-app", redirect, ACCOUNTS_READ, OAuthClient.newState(), OAuthClient.challenge(OAuthClient.newVerifier()));
            OAuthClient.Outcome o = app.signIn(request, "ana", "demo-password");
            line(out, "    " + redirect + ": " + (o.redirected() ? "a code is sent there" : "refused at Keycloak, no redirect (" + o.errorPage() + ")"));
        }
        line(out, "  a prefix or wildcard match would have sent Ana's code to the second, third or fourth address");
        line(out, "  Mallory sends Ana a link to the app's callback carrying Mallory's own code, to sign Ana into Mallory's account:");
        Started mallory = start(app, "ben", ACCOUNTS_READ);
        try {
            OAuthClient.codeIfStateMatches(mallory.outcome(), OAuthClient.newState());
            line(out, "    the app accepted it");
        } catch (IllegalStateException e) {
            line(out, "    the app checks the state against Ana's browser session: refused (" + e.getMessage() + ")");
        }
        Started once = start(app, "ana", ACCOUNTS_READ);
        String code = once.code();
        OAuthClient.TokenReply first = app.token(exchange(code, once.verifier()));
        line(out, "  the code is redeemed once: the access token works: " + api.get("/accounts", first.string("access_token")).status());
        line(out, "  the same code redeemed again: " + refused(app.token(exchange(code, once.verifier()))));
        line(out, "  and the access token from the first redemption, at Keycloak's own check: " + app.userinfo(first.string("access_token")) + "  <- A REUSED CODE ENDS THE SESSION");
        line(out, "  at the API, which checks the signature and the dates itself: " + api.get("/accounts", first.string("access_token")).status() + "  <- A LOCAL CHECK CANNOT KNOW; THE TOKEN LIVES UNTIL IT EXPIRES");
        Started late = start(app, "ana", ACCOUNTS_READ);
        Thread.sleep(6_000);
        line(out, "  a code redeemed 6 seconds after it was issued, in a realm that gives codes 5 seconds: " + refused(app.token(exchange(late.code(), late.verifier()))));
    }

    private static void act4ScopesAndRefresh(StringBuilder out, OAuthClient app, Running api) throws IOException, InterruptedException {
        line(out, "FOUR. Scopes and refresh tokens.");
        Started s = start(app, "ana", ACCOUNTS_READ);
        OAuthClient.TokenReply tokens = app.token(exchange(s.code(), s.verifier()));
        String access = tokens.string("access_token");
        line(out, "  the app asked for accounts.read, Ana agreed to that, and the token carries: " + claims(access).get("scope"));
        line(out, "  GET /accounts: " + api.get("/accounts", access).status());
        line(out, "  POST /payments with the same token: " + api.post("/payments", access).text());
        Map<String, String> greedy = OAuthClient.authorizationRequest("budget-app", REDIRECT, "openid accounts.read payments.write", OAuthClient.newState(), OAuthClient.challenge(OAuthClient.newVerifier()));
        line(out, "  asking for payments.write too, which the app never registered: refused (" + app.signIn(greedy, "ana", "demo-password").error() + ")");
        Map<String, String> refresh = new LinkedHashMap<>(Map.of("grant_type", "refresh_token", "client_id", "budget-app", "refresh_token", tokens.string("refresh_token")));
        OAuthClient.TokenReply renewed = app.token(refresh);
        line(out, "  the access token runs out, so the app refreshes it: a new access token, and a new refresh token: "
                + (renewed.ok() && !renewed.string("refresh_token").equals(tokens.string("refresh_token")) && !renewed.string("access_token").equals(access)));
        line(out, "  the old refresh token, copied by a thief and used again: " + refused(app.token(refresh)));
        Map<String, String> newest = new LinkedHashMap<>(Map.of("grant_type", "refresh_token", "client_id", "budget-app", "refresh_token", renewed.string("refresh_token")));
        line(out, "  the newest refresh token, held by the real app: " + refused(app.token(newest)));
        line(out, "  and the newest access token, at Keycloak's own check: " + app.userinfo(renewed.string("access_token")) + "  <- ONE REUSE ENDS THE SESSION, SO THE THEFT IS CONTAINED");
        Started t = start(app, "ana", ACCOUNTS_READ);
        String fresh = app.token(exchange(t.code(), t.verifier())).string("access_token");
        api.clock().advance(Duration.ofSeconds(331));
        line(out, "  an access token 5 minutes and 31 seconds old, at the API: " + api.get("/accounts", fresh).status());
    }

    private static void act5Practice(StringBuilder out, OAuthClient app) throws IOException, InterruptedException {
        line(out, "FIVE. Current practice, and the bill.");
        Map<String, String> implicit = OAuthClient.authorizationRequest("budget-app", REDIRECT, "openid", OAuthClient.newState(), null);
        implicit.put("response_type", "token");
        line(out, "  the implicit flow, which put the access token in the address bar (response_type=token): refused (" + app.signIn(implicit, "ana", "demo-password").error() + ")");
        line(out, "  the password grant, which gives the app Ana's password: " + refused(app.token(Map.of("grant_type", "password", "client_id", "budget-app", "username", "ana", "password", "demo-password"))));
        line(out, "  both are removed by OAuth 2.0 Security Best Current Practice, RFC 9700, and by OAuth 2.1");
        String verifier = OAuthClient.newVerifier();
        Map<String, String> request = OAuthClient.authorizationRequest("statements-service", STATEMENTS_REDIRECT, ACCOUNTS_READ, OAuthClient.newState(), OAuthClient.challenge(verifier));
        String code = OAuthClient.codeIfStateMatches(app.signIn(request, "ana", "demo-password"), request.get("state"));
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", "statements-service");
        form.put("code", code);
        form.put("redirect_uri", STATEMENTS_REDIRECT);
        form.put("client_secret", "wrong");
        line(out, "  the statements service is a confidential client, so it also authenticates: with a wrong secret: " + refused(app.token(form)));
        form.put("client_secret", "demo-statements-secret");
        form.put("code", "made-up");
        form.put("code_verifier", verifier);
        line(out, "  with its secret, and a code it does not have: " + refused(app.token(form)));
        form.put("code", code);
        form.remove("code_verifier");
        line(out, "  with its secret and a real code but no verifier: " + refused(app.token(form)) + "  (a secret does not replace PKCE)");
        line(out, "  a public app on a phone cannot keep a secret, which is why PKCE, not a secret, protects its code");
        line(out, "  the resource server holds no passwords and no session: it checks the key, the issuer, the audience, the dates and the scope of every token");
        line(out, "  in production the authorization server is operated as a service, with TLS, backups, rotation of its keys, and monitoring of its logins");
    }
}
