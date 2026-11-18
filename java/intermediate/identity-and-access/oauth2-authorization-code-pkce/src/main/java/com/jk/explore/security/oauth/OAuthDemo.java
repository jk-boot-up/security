package com.jk.explore.security.oauth;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Each step prints the request and the response in
 * separate labelled sections. Codes, tokens and states are random, so they are printed as placeholders such as {@code <code>}; the PKCE
 * verifier in the visible flow is the published RFC 7636 example, so its challenge can be worked out step by step.
 */
public class OAuthDemo {

    static final long START = 1_800_000_000L;

    /** RFC 7636 appendix B: the example code verifier and its S256 code challenge. */
    static final String RFC7636_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String RFC7636_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    static final String STATEMENTS_ID = "statements-service";
    static final String STATEMENTS_SECRET = "demo-statements-secret-not-real";

    /** A redirect check that only looks at the start of the address: the mistake exact matching prevents. */
    static final String PREFIX = "https://budget.example";

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

    /** A random value replaced by what it is, so the output is the same on every run. */
    static String shown(String name, String value) {
        return switch (name) {
            case "state", "code" -> "<" + name + ">";
            case "access_token", "refresh_token" -> "<" + name.replace('_', ' ') + ">";
            default -> value;
        };
    }

    /** A URL printed one parameter per line: the address, then ?first and &rest. */
    private static void url(StringBuilder out, String indent, String head, String url) {
        int q = url.indexOf('?');
        line(out, indent + head + url.substring(0, q));
        String sep = "?";
        for (Map.Entry<String, String> e : Urls.queryOf(url).entrySet()) {
            line(out, indent + "  " + sep + e.getKey() + "=" + shown(e.getKey(), e.getValue()));
            sep = "&";
        }
    }

    /** A refusal: the OAuth error code, then its reason on the next line. */
    private static void refused(StringBuilder out, String indent, OAuthException e) {
        line(out, indent + "-> refused: " + e.error());
        line(out, indent + "   " + e.getMessage());
    }

    /** The error the authorization endpoint sent back, or null when it issued a code. */
    static OAuthException refusal(AuthorizationServer.AuthorizeResult result) {
        if (!result.redirected()) {
            return new OAuthException("no redirect", result.errorPage());
        }
        Map<String, String> back = Urls.queryOf(result.redirectTo());
        return back.containsKey("error") ? new OAuthException(back.get("error"), back.get("error_description")) : null;
    }

    private static Map<String, String> query(String url) {
        return Urls.queryOf(url);
    }

    private static void act1Flow(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. The authorization code flow, step by step.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BankApi api = new BankApi(server);
        Map<String, String> sent = new LinkedHashMap<>();
        BudgetApp app = new BudgetApp(form -> {
            sent.putAll(form);
            return server.token(form);
        });
        String authorize = app.startLogin("ana-browser", BankApi.READ, RFC7636_VERIFIER);
        line(out, "  STEP 1, THE REQUEST (the app sends Ana's browser to the bank):");
        url(out, "    ", "GET ", authorize);
        line(out, "  STEP 2, AT THE BANK (never at the app):");
        line(out, "    Ana signs in with her bank password");
        line(out, "    the bank asks: budget-app wants to read your accounts");
        line(out, "    Ana agrees to " + query(authorize).get("scope"));
        AuthorizationServer.AuthorizeResult result = server.authorize(query(authorize), "ana", true);
        line(out, "  STEP 3, THE RESPONSE (the bank sends the browser back):");
        url(out, "    ", "302 Location: ", result.redirectTo());
        String code = query(result.redirectTo()).get("code");
        line(out, "    the code: " + code.length() + " random characters, works for " + AuthorizationServer.CODE_LIFETIME + " seconds, once");
        AuthorizationServer.Tokens tokens;
        try {
            tokens = app.callback("ana-browser", query(result.redirectTo()));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  STEP 4, THE REQUEST (the app calls the bank directly):");
        line(out, "    POST https://bank.example/token");
        for (Map.Entry<String, String> e : sent.entrySet()) {
            line(out, "      " + e.getKey() + "=" + shown(e.getKey(), e.getValue()));
        }
        line(out, "  STEP 5, THE RESPONSE (to the app, not to the browser):");
        line(out, "    access_token=" + shown("access_token", tokens.accessToken()) + "  expires_in=" + tokens.expiresIn());
        line(out, "    refresh_token=" + shown("refresh_token", tokens.refreshToken()));
        line(out, "    scope=" + String.join(" ", tokens.scope()));
        BankApi.Answer answer = api.call("GET", "/accounts", "Bearer " + tokens.accessToken());
        line(out, "  STEP 6, THE APP CALLS THE BANK'S API:");
        line(out, "    GET /accounts  Authorization: Bearer <access token>");
        line(out, "    -> " + answer.status() + " " + answer.body());
        line(out, "  WHO SAW WHAT:");
        line(out, "    the app never saw Ana's password");
        line(out, "    the browser never saw the access token");
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
            line(out, "  THE SAME FLOW OVER A REAL LOCAL HTTP SERVER:");
            line(out, "    authorize " + redirect.statusCode() + ", " + status);
        }
    }

    private static void act2Pkce(StringBuilder out) {
        line(out, "TWO. PKCE: a code is useless without the verifier.");
        line(out, "  THE VERIFIER (made by the app for one sign-in, kept secret):");
        line(out, "    " + RFC7636_VERIFIER);
        line(out, "    " + RFC7636_VERIFIER.length() + " characters from A-Z a-z 0-9 - . _ ~ (the RFC 7636 example)");
        byte[] ascii = ChallengeSteps.ascii(RFC7636_VERIFIER);
        byte[] hash = ChallengeSteps.sha256(ascii);
        String challenge = ChallengeSteps.base64Url(hash);
        line(out, "  HOW THE S256 CHALLENGE IS MADE:");
        line(out, "    1. the " + RFC7636_VERIFIER.length() + " characters as " + ascii.length + " ASCII bytes:");
        line(out, "       " + ChallengeSteps.hex(ascii, 0, 16) + " ...");
        line(out, "    2. SHA-256 of those bytes, always " + hash.length + " bytes:");
        line(out, "       " + ChallengeSteps.hex(hash, 0, 16));
        line(out, "       " + ChallengeSteps.hex(hash, 16, 32));
        line(out, "    3. base64url: every 3 bytes become 4 characters of 6 bits");
        line(out, "       " + ChallengeSteps.firstGroup(hash));
        line(out, "    4. " + hash.length + " bytes become " + challenge.length() + " characters, with no = padding:");
        line(out, "       " + challenge);
        line(out, "    the same as the challenge RFC 7636 publishes: " + challenge.equals(RFC7636_CHALLENGE));
        line(out, "    the same as Pkce.challenge, which uses Java's Base64: " + challenge.equals(Pkce.challenge(RFC7636_VERIFIER)));

        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BudgetApp app = new BudgetApp(server::token);
        String authorize = app.startLogin("ana-browser", BankApi.READ, RFC7636_VERIFIER);
        line(out, "  WHAT TRAVELS THROUGH THE BROWSER (step 1):");
        line(out, "    the challenge: " + authorize.contains(RFC7636_CHALLENGE));
        line(out, "    the verifier: " + authorize.contains(RFC7636_VERIFIER));
        String code = query(server.authorize(query(authorize), "ana", true).redirectTo()).get("code");
        line(out, "  THE LEAK (the code is copied from a log or a browser history):");
        line(out, "    another app redeems it with no verifier");
        redeem(out, server, code, null);
        line(out, "    another app redeems it with a verifier it made up");
        redeem(out, server, code, Pkce.newVerifier());
        line(out, "    the budgeting app redeems it with the real verifier");
        redeem(out, server, code, RFC7636_VERIFIER);
        line(out, "  WEAKER REQUESTS, REFUSED BY THE BANK:");
        Map<String, String> plain = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        plain.put("code_challenge_method", "plain");
        line(out, "    code_challenge_method=plain (the verifier itself travels)");
        refused(out, "      ", refusal(server.authorize(plain, "ana", true)));
        Map<String, String> none = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        none.remove("code_challenge");
        none.remove("code_challenge_method");
        line(out, "    no code_challenge at all");
        refused(out, "      ", refusal(server.authorize(none, "ana", true)));
    }

    private static Map<String, String> codeForm(String code, String verifier) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", BudgetApp.REDIRECT_URI);
        form.put("client_id", BudgetApp.CLIENT_ID);
        if (verifier != null) {
            form.put("code_verifier", verifier);
        }
        return form;
    }

    /** Redeems a code and prints the result under the line that describes the attempt. */
    private static AuthorizationServer.Tokens redeem(StringBuilder out, AuthorizationServer server, String code, String verifier) {
        return token(out, server, codeForm(code, verifier));
    }

    private static AuthorizationServer.Tokens token(StringBuilder out, AuthorizationServer server, Map<String, String> form) {
        try {
            AuthorizationServer.Tokens tokens = server.token(form);
            line(out, "      -> tokens issued");
            return tokens;
        } catch (OAuthException e) {
            refused(out, "      ", e);
            return null;
        }
    }

    private static void act3RedirectsAndCodes(StringBuilder out) {
        line(out, "THREE. Exact redirect addresses, state, and codes that work once.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BudgetApp app = new BudgetApp(server::token);
        line(out, "  THE REGISTERED ADDRESS (the bank compares it exactly):");
        line(out, "    " + BudgetApp.REDIRECT_URI);
        line(out, String.format("  %-45s%-9s%s", "THE ADDRESS IN THE REQUEST:", "EXACT", "PREFIX"));
        int sentAway = 0;
        for (String candidate : List.of(BudgetApp.REDIRECT_URI, "https://budget.example/callback/../steal", "https://budget.example.evil.test/callback",
                "https://budget.example/callback?next=evil", "https://BUDGET.example/callback")) {
            Map<String, String> params = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
            params.put("redirect_uri", candidate);
            AuthorizationServer.AuthorizeResult result = server.authorize(params, "ana", true);
            boolean prefix = candidate.startsWith(PREFIX);
            if (prefix && !candidate.equals(BudgetApp.REDIRECT_URI)) {
                sentAway++;
            }
            line(out, String.format("    %-43s%-9s%s", candidate, result.redirected() ? "code" : "refused", prefix ? "code" : "refused"));
        }
        line(out, "  a prefix check on " + PREFIX + " sends the code away " + sentAway + " times");

        String mallory = app.startLogin("mallory-browser", BankApi.READ);
        String malloryCallback = server.authorize(query(mallory), "mallory", true).redirectTo();
        app.startLogin("ana-browser", BankApi.READ);
        line(out, "  THE ATTACK (login forgery):");
        line(out, "    Ana's browser opens a callback link with Mallory's code and state");
        line(out, "  THE APP'S CHECK (the state against Ana's browser session):");
        try {
            app.callback("ana-browser", query(malloryCallback));
            line(out, "    -> accepted");
        } catch (OAuthException e) {
            refused(out, "    ", e);
        }

        String authorize = app.startLogin("ana-browser", BankApi.READ);
        Map<String, String> back = query(server.authorize(query(authorize), "ana", true).redirectTo());
        AuthorizationServer.Tokens tokens;
        try {
            tokens = app.callback("ana-browser", back);
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
        BankApi api = new BankApi(server);
        line(out, "  ONE CODE, REDEEMED TWICE:");
        line(out, "    first: tokens issued, GET /accounts -> " + api.call("GET", "/accounts", "Bearer " + tokens.accessToken()).status());
        line(out, "    second, by whoever copied the code:");
        redeem(out, server, back.get("code"), Pkce.newVerifier());
        line(out, "    the first access token now: GET /accounts -> " + api.call("GET", "/accounts", "Bearer " + tokens.accessToken()).status());

        String later = app.startLogin("ana-browser", BankApi.READ, RFC7636_VERIFIER);
        String lateCode = query(server.authorize(query(later), "ana", true).redirectTo()).get("code");
        clock.advance(AuthorizationServer.CODE_LIFETIME + 1);
        line(out, "  ONE CODE, REDEEMED " + (AuthorizationServer.CODE_LIFETIME + 1) + " SECONDS AFTER IT WAS ISSUED:");
        line(out, "    by the budgeting app, with the real verifier");
        redeem(out, server, lateCode, RFC7636_VERIFIER);
    }

    private static void act4Scopes(StringBuilder out) {
        line(out, "FOUR. Scopes and refresh tokens.");
        SimClock clock = new SimClock(START);
        AuthorizationServer server = bank(clock);
        BankApi api = new BankApi(server);
        BudgetApp app = new BudgetApp(server::token);
        AuthorizationServer.Tokens first = signIn(server, app);
        line(out, "  THE SCOPE (asked for, agreed to, carried by the token):");
        line(out, "    " + String.join(" ", first.scope()));
        line(out, "  THE API CALLS WITH THAT TOKEN:");
        line(out, "    GET /accounts -> " + api.call("GET", "/accounts", "Bearer " + first.accessToken()).status());
        BankApi.Answer pay = api.call("POST", "/payments", "Bearer " + first.accessToken());
        line(out, "    POST /payments -> " + pay.status() + " " + pay.body());
        Map<String, String> greedy = new LinkedHashMap<>(query(app.startLogin("ana-browser", BankApi.READ)));
        greedy.put("scope", BankApi.READ + " " + BankApi.PAY);
        line(out, "  ASKING FOR " + BankApi.PAY + " TOO (never registered):");
        refused(out, "    ", refusal(server.authorize(greedy, "ana", true)));

        AuthorizationServer.Tokens second = refresh(server, first.refreshToken());
        line(out, "  THE REFRESH (access token A1 ran out; the app sends R1):");
        line(out, "    R1 -> a new access token A2 and a new refresh token R2");
        line(out, "    R2 differs from R1: " + !second.refreshToken().equals(first.refreshToken()));
        line(out, "  THE THEFT (a thief copied R1 and replays it):");
        line(out, "    R1 again");
        refreshShown(out, server, first.refreshToken());
        line(out, "    R2, held by the real app");
        refreshShown(out, server, second.refreshToken());
        line(out, "    A2: GET /accounts -> " + api.call("GET", "/accounts", "Bearer " + second.accessToken()).status()
                + "  <- THE WHOLE FAMILY IS REVOKED");

        AuthorizationServer.Tokens fresh = signIn(server, app);
        clock.advance(AuthorizationServer.ACCESS_LIFETIME);
        line(out, "  AN ACCESS TOKEN " + AuthorizationServer.ACCESS_LIFETIME + " SECONDS OLD:");
        line(out, "    GET /accounts -> " + api.call("GET", "/accounts", "Bearer " + fresh.accessToken()).status());
    }

    private static AuthorizationServer.Tokens signIn(AuthorizationServer server, BudgetApp app) {
        try {
            return app.callback("ana-browser", query(server.authorize(query(app.startLogin("ana-browser", BankApi.READ)), "ana", true).redirectTo()));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AuthorizationServer.Tokens refresh(AuthorizationServer server, String refreshToken) {
        try {
            return server.token(refreshForm(refreshToken));
        } catch (OAuthException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void refreshShown(StringBuilder out, AuthorizationServer server, String refreshToken) {
        token(out, server, refreshForm(refreshToken));
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
        line(out, "  THE IMPLICIT FLOW (response_type=token: a token in the address):");
        refused(out, "    ", refusal(server.authorize(implicit, "ana", true)));
        Map<String, String> password = new LinkedHashMap<>();
        password.put("grant_type", "password");
        password.put("client_id", BudgetApp.CLIENT_ID);
        password.put("username", "ana");
        password.put("password", "<Ana's password>");
        line(out, "  THE PASSWORD GRANT (the app would hold Ana's password):");
        try {
            server.token(password);
            line(out, "    -> tokens issued");
        } catch (OAuthException e) {
            refused(out, "    ", e);
        }
        line(out, "  both are removed by RFC 9700 and by OAuth 2.1");
        line(out, "  A CONFIDENTIAL CLIENT (" + STATEMENTS_ID + ", on a server):");
        line(out, "    a wrong client secret");
        token(out, server, statementsForm("not-the-secret"));
        line(out, "    the right secret, but a code it was never given");
        token(out, server, statementsForm(STATEMENTS_SECRET));
        line(out, "  a phone app cannot keep a secret, so PKCE protects its code");
        line(out, "  WHAT THIS SMALL SERVER LEAVES OUT:");
        line(out, "    discovery, signed tokens, consent records, key rotation");
        line(out, "    in production, run a real authorization server");
    }

    private static Map<String, String> statementsForm(String secret) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", "no-such-code");
        form.put("redirect_uri", "https://statements.bank.example/callback");
        form.put("client_id", STATEMENTS_ID);
        form.put("client_secret", secret);
        return form;
    }
}
