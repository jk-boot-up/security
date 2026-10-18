package com.jk.explore.security.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The budgeting app, a public OAuth client. For each sign-in it makes a fresh state and a fresh PKCE verifier, and remembers both against
 * the browser session that started it. A callback is accepted only if its state is the one that browser was given.
 */
final class BudgetApp {

    static final String CLIENT_ID = "budget-app";
    static final String REDIRECT_URI = "https://budget.example/callback";
    static final String AUTHORIZE_URL = "https://bank.example/authorize";

    private record Pending(String state, String verifier) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pendingBySession = new HashMap<>();
    private final Map<String, AuthorizationServer.Tokens> tokensBySession = new HashMap<>();
    private final TokenEndpoint tokenEndpoint;

    /** The token endpoint is a function, so the same app can call the bank in-process or over HTTP. */
    BudgetApp(TokenEndpoint tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    /** Starts a sign-in for one browser session and returns the address the browser is sent to. */
    String startLogin(String browserSession, String scope) {
        return startLogin(browserSession, scope, Pkce.newVerifier());
    }

    /** The same, with a given verifier, so the demo can use the published RFC 7636 example. */
    String startLogin(String browserSession, String scope, String verifier) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String state = Pkce.base64Url(bytes);
        pendingBySession.put(browserSession, new Pending(state, verifier));
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", CLIENT_ID);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("scope", scope);
        params.put("state", state);
        params.put("code_challenge", Pkce.challenge(verifier));
        params.put("code_challenge_method", Pkce.S256);
        return AUTHORIZE_URL + "?" + Urls.form(params);
    }

    /** The redirect back from the bank, as it reaches the app through one browser session. */
    AuthorizationServer.Tokens callback(String browserSession, Map<String, String> query) throws OAuthException {
        Pending pending = pendingBySession.remove(browserSession);
        String state = query.get("state");
        if (pending == null || state == null
                || !MessageDigest.isEqual(pending.state().getBytes(StandardCharsets.US_ASCII), state.getBytes(StandardCharsets.US_ASCII))) {
            throw new OAuthException("invalid_state", "this browser did not start this sign-in");
        }
        if (query.containsKey("error")) {
            throw new OAuthException(query.get("error"), query.getOrDefault("error_description", ""));
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", query.getOrDefault("code", ""));
        form.put("redirect_uri", REDIRECT_URI);
        form.put("client_id", CLIENT_ID);
        form.put("code_verifier", pending.verifier());
        AuthorizationServer.Tokens tokens = tokenEndpoint.call(form);
        tokensBySession.put(browserSession, tokens);
        return tokens;
    }

    /** The pending state of a session, for tests and the demo; never shown to anyone else. */
    String stateFor(String browserSession) {
        Pending pending = pendingBySession.get(browserSession);
        return pending == null ? null : pending.state();
    }
}
