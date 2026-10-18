package com.jk.explore.security.oauth;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Codes, tokens and states are random, so they are
 * never printed: messages show what each part is, and the published RFC 7636 example is the only secret shown in full.
 */
public class OAuthDemo {

    static final long START = 1_800_000_000L;

    /** RFC 7636 appendix B: the example code verifier and its S256 code challenge. */
    static final String RFC7636_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String RFC7636_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    static final String STATEMENTS_ID = "statements-service";
    static final String STATEMENTS_SECRET = "demo-statements-secret-not-real";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Flow(out);
        act2Pkce(out);
        act3RedirectsAndCodes(out);
        act4Scopes(out);
        act5Practice(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The bank's authorization server with its two registered clients. */
    static AuthorizationServer bank(SimClock clock) {
        return new AuthorizationServer(clock)
                .register(ClientRegistration.publicClient(BudgetApp.CLIENT_ID, List.of(BudgetApp.REDIRECT_URI), Set.of(BankApi.READ)))
                .register(ClientRegistration.confidentialClient(STATEMENTS_ID, List.of("https://statements.bank.example/callback"),
                        Set.of(BankApi.READ), STATEMENTS_SECRET));
    }

    /** A message with its secret values replaced by what they are, so the output is the same on every run. */
    static String elide(String message) {
        String out = message;
        for (String name : List.of("state", "code_challenge", "code", "code_verifier")) {
            out = out.replaceAll("([?&])" + name + "=[^&]*", "$1" + name + "=<" + name.replace('_', ' ') + ">");
        }
        return out.replace("%3A", ":").replace("%2F", "/").replace("%2B", "+");
    }

    private static Map<String, String> query(String url) {
        return Urls.queryOf(url);
    }

    private static void act1Flow(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. The authorization code flow, step by step.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BankApi api = new BankApi(server);
        BudgetApp app = new BudgetApp(server::token);
        String authorize = app.startLogin("ana-browser", BankApi.READ);
        line(out, "  1. the budgeting app sends Ana's browser to the bank:");
        line(out, "     GET " + elide(authorize));
        line(out, "  2. Ana signs in at the bank, never at the app, and is asked: budget-app wants to read your accounts (" + BankApi.READ + "). Ana agrees");
        AuthorizationServer.AuthorizeResult result = server.authorize(query(authorize), "ana", true);
        line(out, "  3. the bank sends the browser back: 302 " + elide(result.redirectTo()));
        String code = query(result.redirectTo()).get("code");
        line(out, "     the code is " + code.length() + " random characters and works for " + AuthorizationServer.CODE_LIFETIME + " seconds, once");
        line(out, "  4. the app checks the state, then calls the bank directly, not through the browser:");
        line(out, "     POST https://bank.example/token grant_type=authorization_code&code=<code>&redirect_uri=" + BudgetApp.REDIRECT_URI
                + "&client_id=" + BudgetApp.CLIENT_ID + "&code_verifier=<code verifier>");
        AuthorizationServer.Tokens tokens;
        try {
            tokens = app.callback("ana-browser", query(result.redirectTo()));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  5. the bank answers with an access token for " + tokens.expiresIn() + " seconds, scope " + String.join(" ", tokens.scope())
                + ", and a refresh token");
        BankApi.Answer answer = api.call("GET", "/accounts", "Bearer " + tokens.accessToken());
        line(out, "  6. the app reads Ana's accounts: GET /accounts with the access token: " + answer.status() + ", " + answer.body());
        line(out, "  the app never saw Ana's password, and the browser never saw the access token");
        try (LoopbackBank http = new LoopbackBank(server, api)) {
            BudgetApp overHttp = new BudgetApp(http::token);
            HttpResponse<String> redirect = http.authorize(overHttp.startLogin("ana-browser", BankApi.READ), "ana", true);
            String status;
            try {
                AuthorizationServer.Tokens real = overHttp.callback("ana-browser", LoopbackBank.locationQuery(redirect));
                status = "token 200, accounts " + http.accounts(real.accessToken());
            } catch (OAuthException e) {
                status = "token refused (" + e.error() + ")";
            }
            line(out, "  the same flow over a real local HTTP server: authorize " + redirect.statusCode() + ", " + status);
        }
    }

    private static void act2Pkce(StringBuilder out) {
        line(out, "TWO. PKCE: a code is useless without the verifier.");
        line(out, "  the RFC 7636 example verifier " + RFC7636_VERIFIER);
        String challenge = Pkce.challenge(RFC7636_VERIFIER);
        line(out, "  gives the S256 challenge " + challenge + ": the published value: " + challenge.equals(RFC7636_CHALLENGE));
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BudgetApp app = new BudgetApp(server::token);
        String authorize = app.startLogin("ana-browser", BankApi.READ, RFC7636_VERIFIER);
        line(out, "  only the challenge travels through the browser: the request carries it: " + authorize.contains(RFC7636_CHALLENGE)
                + ", and the verifier: " + authorize.contains(RFC7636_VERIFIER));
        String code = query(server.authorize(query(authorize), "ana", true).redirectTo()).get("code");
        line(out, "  the code leaks, say through a log or a browser history, and another app redeems it:");
        line(out, "    with no verifier: " + redeem(server, code, null));
        line(out, "    with a verifier it made up: " + redeem(server, code, Pkce.newVerifier()));
        line(out, "  the budgeting app redeems it with the real verifier: " + redeem(server, code, RFC7636_VERIFIER));
        Map<String, String> plain = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        plain.put("code_challenge_method", "plain");
        line(out, "  a request with code_challenge_method=plain, which sends the verifier itself: " + refusal(server.authorize(plain, "ana", true)));
        Map<String, String> none = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        none.remove("code_challenge");
        none.remove("code_challenge_method");
        line(out, "  a request with no challenge at all: " + refusal(server.authorize(none, "ana", true)));
    }

    private static String redeem(AuthorizationServer server, String code, String verifier) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", BudgetApp.REDIRECT_URI);
        form.put("client_id", BudgetApp.CLIENT_ID);
        if (verifier != null) {
            form.put("code_verifier", verifier);
        }
        try {
            server.token(form);
            return "tokens issued";
        } catch (OAuthException e) {
            return "refused (" + e.shown() + ")";
        }
    }

    private static String refusal(AuthorizationServer.AuthorizeResult result) {
        if (!result.redirected()) {
            return "refused at the bank, no redirect (" + result.errorPage() + ")";
        }
        Map<String, String> back = query(result.redirectTo());
        return back.containsKey("error") ? "refused (" + back.get("error") + ": " + back.get("error_description") + ")" : "a code was issued";
    }

    private static void act3RedirectsAndCodes(StringBuilder out) {
        line(out, "THREE. Exact redirect addresses, state, and codes that work once.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BudgetApp app = new BudgetApp(server::token);
        line(out, "  the budgeting app registered exactly " + BudgetApp.REDIRECT_URI);
        for (String candidate : List.of(BudgetApp.REDIRECT_URI, "https://budget.example/callback/../steal", "https://budget.example.evil.test/callback",
                "https://budget.example/callback?next=evil", "https://BUDGET.example/callback")) {
            Map<String, String> params = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
            params.put("redirect_uri", candidate);
            AuthorizationServer.AuthorizeResult result = server.authorize(params, "ana", true);
            line(out, "    " + candidate + ": " + (result.redirected() ? "a code is sent there" : "refused at the bank, no redirect"));
        }
        line(out, "  a prefix or wildcard match would have sent Ana's code to the second, third or fourth address");

        String mallory = app.startLogin("mallory-browser", BankApi.READ);
        String malloryCallback = server.authorize(query(mallory), "mallory", true).redirectTo();
        app.startLogin("ana-browser", BankApi.READ);
        String forced;
        try {
            app.callback("ana-browser", query(malloryCallback));
            forced = "accepted";
        } catch (OAuthException e) {
            forced = "refused (" + e.shown() + ")";
        }
        line(out, "  Mallory sends Ana a link to the app's callback carrying Mallory's own code, to sign Ana into Mallory's account:");
        line(out, "    the app checks the state against Ana's browser session: " + forced);

        String authorize = app.startLogin("ana-browser", BankApi.READ);
        String callback = server.authorize(query(authorize), "ana", true).redirectTo();
        Map<String, String> back = query(callback);
        AuthorizationServer.Tokens tokens;
        try {
            tokens = app.callback("ana-browser", back);
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        BankApi api = new BankApi(server);
        line(out, "  the code is redeemed once: the access token works: " + api.call("GET", "/accounts", "Bearer " + tokens.accessToken()).status());
        line(out, "  the same code redeemed again: " + redeemWithApp(server, back.get("code")));
        line(out, "  and the access token from the first redemption is now: " + api.call("GET", "/accounts", "Bearer " + tokens.accessToken()).status()
                + "  <- A REUSED CODE MEANS IT LEAKED, SO EVERYTHING FROM IT IS REVOKED");

        String later = app.startLogin("ana-browser", BankApi.READ);
        String lateCode = query(server.authorize(query(later), "ana", true).redirectTo()).get("code");
        clock.advance(AuthorizationServer.CODE_LIFETIME + 1);
        line(out, "  a code redeemed " + (AuthorizationServer.CODE_LIFETIME + 1) + " seconds after it was issued: " + redeemWithApp(server, lateCode));
    }

    /** Redeems a code as an attacker would, without the app's verifier for that code. */
    private static String redeemWithApp(AuthorizationServer server, String code) {
        return redeem(server, code, Pkce.newVerifier());
    }

    private static void act4Scopes(StringBuilder out) {
        line(out, "FOUR. Scopes and refresh tokens.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BankApi api = new BankApi(server);
        BudgetApp app = new BudgetApp(server::token);
        AuthorizationServer.Tokens tokens;
        try {
            tokens = app.callback("ana-browser", query(server.authorize(query(app.startLogin("ana-browser", BankApi.READ)), "ana", true).redirectTo()));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  the app asked for " + BankApi.READ + ", Ana agreed to that, and the token carries: " + String.join(" ", tokens.scope()));
        line(out, "  GET /accounts: " + api.call("GET", "/accounts", "Bearer " + tokens.accessToken()).status());
        BankApi.Answer pay = api.call("POST", "/payments", "Bearer " + tokens.accessToken());
        line(out, "  POST /payments with the same token: " + pay.status() + " " + pay.body());
        Map<String, String> greedy = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        greedy.put("scope", BankApi.READ + " " + BankApi.PAY);
        line(out, "  asking for " + BankApi.PAY + " too, which the app never registered: " + refusal(server.authorize(greedy, "ana", true)));

        String first = tokens.refreshToken();
        AuthorizationServer.Tokens second = refresh(server, first);
        line(out, "  the access token runs out, so the app refreshes it: a new access token, and a new refresh token: " + !second.refreshToken().equals(first));
        line(out, "  the old refresh token, copied by a thief and used again: " + refreshShown(server, first));
        line(out, "  the newest refresh token, held by the real app: " + refreshShown(server, second.refreshToken()));
        line(out, "  and the newest access token: " + api.call("GET", "/accounts", "Bearer " + second.accessToken()).status()
                + "  <- ONE REUSE REVOKES THE WHOLE FAMILY, SO THE THEFT IS CONTAINED");

        AuthorizationServer.Tokens fresh;
        try {
            fresh = app.callback("ana-browser", query(server.authorize(query(app.startLogin("ana-browser", BankApi.READ)), "ana", true).redirectTo()));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        clock.advance(AuthorizationServer.ACCESS_LIFETIME);
        line(out, "  an access token " + AuthorizationServer.ACCESS_LIFETIME + " seconds old: " + api.call("GET", "/accounts", "Bearer " + fresh.accessToken()).status());
    }

    private static AuthorizationServer.Tokens refresh(AuthorizationServer server, String refreshToken) {
        try {
            return server.token(refreshForm(refreshToken));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String refreshShown(AuthorizationServer server, String refreshToken) {
        try {
            server.token(refreshForm(refreshToken));
            return "accepted";
        } catch (OAuthException e) {
            return "refused (" + e.shown() + ")";
        }
    }

    private static Map<String, String> refreshForm(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", BudgetApp.CLIENT_ID);
        return form;
    }

    private static void act5Practice(StringBuilder out) {
        line(out, "FIVE. Current practice, and the bill.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BudgetApp app = new BudgetApp(server::token);
        Map<String, String> implicit = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        implicit.put("response_type", "token");
        line(out, "  the implicit flow, which put the access token in the address bar (response_type=token): " + refusal(server.authorize(implicit, "ana", true)));
        Map<String, String> password = new LinkedHashMap<>();
        password.put("grant_type", "password");
        password.put("client_id", BudgetApp.CLIENT_ID);
        password.put("username", "ana");
        password.put("password", "<Ana's password>");
        line(out, "  the password grant, which gives the app Ana's password: " + tokenShown(server, password));
        line(out, "  both are removed by OAuth 2.0 Security Best Current Practice, RFC 9700, and by OAuth 2.1");
        Map<String, String> wrongSecret = clientCredentialsLike("not-the-secret");
        line(out, "  the statements service is a confidential client, so it also authenticates: with a wrong secret: " + tokenShown(server, wrongSecret));
        line(out, "  with its secret, and still a code and a verifier it does not have: " + tokenShown(server, clientCredentialsLike(STATEMENTS_SECRET)));
        line(out, "  a public app on a phone cannot keep a secret, which is why PKCE, not a secret, protects its code");
        line(out, "  this server leaves out discovery, signed tokens, consent records, key rotation and account recovery:");
        line(out, "  in production, run a real authorization server, as in the -with-keycloak version");
    }

    private static Map<String, String> clientCredentialsLike(String secret) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", "no-such-code");
        form.put("redirect_uri", "https://statements.bank.example/callback");
        form.put("client_id", STATEMENTS_ID);
        form.put("client_secret", secret);
        return form;
    }

    private static String tokenShown(AuthorizationServer server, Map<String, String> form) {
        try {
            server.token(form);
            return "tokens issued";
        } catch (OAuthException e) {
            return "refused (" + e.shown() + ")";
        }
    }
}
