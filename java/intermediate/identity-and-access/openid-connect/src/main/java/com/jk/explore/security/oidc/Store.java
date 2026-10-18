package com.jk.explore.security.oidc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The online store as an OpenID Connect relying party: "sign in with the bank". It binds a state and a nonce to the browser before
 * sending it to the provider, validates the ID token it gets back, and identifies the customer by issuer and subject. State, nonce
 * and session ids are random and are never printed.
 */
final class Store {

    static final String CLIENT_ID = "store-web";
    static final String REDIRECT_URI = "https://shop.example/callback";
    static final String POST_LOGOUT_URI = "https://shop.example/signed-out";

    /** How accounts are keyed: the right way, or by email address, which is how account takeovers happen. */
    enum AccountKey { ISSUER_AND_SUBJECT, EMAIL }

    record Account(String key, String issuer, String subject, String email) {
    }

    /** A browser with its cookie for the store, and the state and nonce of a sign-in that has started. */
    static final class Browser {
        private final String name;
        private String sessionCookie;
        private Pending pending;

        Browser(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }
    }

    private record Pending(String issuer, String state, String nonce) {
    }

    private record Session(Account account, String issuer, String providerSid, String idToken) {
    }

    private final Map<String, ProviderConfig> configs = new LinkedHashMap<>();
    private final Map<String, Provider> backChannel = new HashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final SimClock clock;
    private final AccountKey accountKey;
    private final SecureRandom random = new SecureRandom();

    Store(SimClock clock, AccountKey accountKey) {
        this.clock = clock;
        this.accountKey = accountKey;
    }

    /** Trusts one provider: its configuration comes from discovery, and the token endpoint is called directly, server to server. */
    void trust(ProviderConfig config, Provider tokenEndpoint) {
        configs.put(config.issuer(), config);
        backChannel.put(config.issuer(), tokenEndpoint);
    }

    void refreshKeys(ProviderConfig config) {
        configs.put(config.issuer(), config);
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Starts a sign-in: a fresh state and nonce are kept for this browser, and the browser is sent to the provider. */
    Provider.AuthorizationRequest beginLogin(Browser browser, String issuer, String scope) {
        Pending pending = new Pending(issuer, randomValue(), randomValue());
        browser.pending = pending;
        return new Provider.AuthorizationRequest(issuer, CLIENT_ID, REDIRECT_URI, scope, pending.state(), pending.nonce());
    }

    /** The browser comes back with a code. Returns the account it is now signed in to. */
    Account callback(Browser browser, Provider.AuthorizationResponse response) throws InvalidTokenException {
        Pending pending = browser.pending;
        browser.pending = null;
        if (pending == null || !equal(pending.state(), response.state())) {
            throw new InvalidTokenException("state does not match this browser");
        }
        Provider.TokenResponse tokens = backChannel.get(pending.issuer()).token(response.code(), CLIENT_ID);
        if (tokens.idToken() == null) {
            throw new InvalidTokenException("no ID token: the reply gives API access, not a sign-in");
        }
        return signIn(browser, tokens.idToken(), pending);
    }

    /** What a sloppy store does: takes any token it is handed as proof of sign-in, checked the same strict way. */
    Account signInWithToken(Browser browser, String token, String issuer, String nonce) throws InvalidTokenException {
        return signIn(browser, token, new Pending(issuer, "", nonce));
    }

    private Account signIn(Browser browser, String idToken, Pending pending) throws InvalidTokenException {
        ProviderConfig config = configs.get(pending.issuer());
        IdToken token = IdTokenValidator.validate(idToken, config, CLIENT_ID, pending.nonce(), clock.now());
        String email = token.emailVerified() ? token.email() : null;
        String key = accountKey == AccountKey.EMAIL ? String.valueOf(token.email()) : token.identity();
        Account account = accounts.computeIfAbsent(key, k -> new Account(k, token.issuer(), token.subject(), email));
        String sessionId = randomValue();
        sessions.put(sessionId, new Session(account, token.issuer(), token.sid(), idToken));
        browser.sessionCookie = sessionId;
        return account;
    }

    Optional<Account> signedIn(Browser browser) {
        Session session = browser.sessionCookie == null ? null : sessions.get(browser.sessionCookie);
        return session == null ? Optional.empty() : Optional.of(session.account());
    }

    int accountCount() {
        return accounts.size();
    }

    int sessionCount() {
        return sessions.size();
    }

    /** RP-initiated logout: the store's session ends here, and the browser is sent to the provider's end-session endpoint. */
    String logout(Browser browser) {
        Session session = browser.sessionCookie == null ? null : sessions.remove(browser.sessionCookie);
        browser.sessionCookie = null;
        if (session == null) {
            return POST_LOGOUT_URI;
        }
        ProviderConfig config = configs.get(session.issuer());
        return config.endSessionEndpoint() + "?id_token_hint=" + session.idToken()
                + "&post_logout_redirect_uri=" + URLEncoder.encode(POST_LOGOUT_URI, StandardCharsets.UTF_8)
                + "&state=" + randomValue();
    }

    /**
     * Back-channel logout (OpenID Connect Back-Channel Logout 1.0, section 2.6): the provider tells the store directly that a
     * session has ended. The logout token must be typed, carry the logout event, name a session or subject, and have no nonce.
     * Returns how many of the store's sessions ended.
     */
    int backchannelLogout(String logoutToken) throws InvalidTokenException {
        Jws.Parts parts = Jws.parse(logoutToken);
        if (!"EdDSA".equals(parts.header().get("alg"))) {
            throw new InvalidTokenException("algorithm is not EdDSA");
        }
        if (!"logout+jwt".equals(parts.header().get("typ"))) {
            throw new InvalidTokenException("not a logout token");
        }
        Map<String, Object> unverified = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));
        ProviderConfig config = unverified.get("iss") instanceof String iss ? configs.get(iss) : null;
        if (config == null) {
            throw new InvalidTokenException("unknown issuer");
        }
        PublicKey key = parts.header().get("kid") instanceof String kid ? config.keys().find(kid).orElse(null) : null;
        if (key == null || !Ed25519.verify(key, parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("signature does not match");
        }
        Map<String, Object> claims = unverified;
        if (!IdTokenValidator.audience(claims.get("aud")).contains(CLIENT_ID)) {
            throw new InvalidTokenException("audience does not include this client");
        }
        long iat = IdTokenValidator.number(claims, "iat");
        if (Math.abs(clock.now() - iat) > IdTokenValidator.MAX_AGE_SECONDS) {
            throw new InvalidTokenException("issued too long ago");
        }
        if (!(claims.get("jti") instanceof String)) {
            throw new InvalidTokenException("no jti");
        }
        if (!(claims.get("events") instanceof Map<?, ?> events) || !(events.get(Provider.LOGOUT_EVENT) instanceof Map<?, ?>)) {
            throw new InvalidTokenException("no logout event");
        }
        if (claims.containsKey("nonce")) {
            throw new InvalidTokenException("a logout token must not have a nonce");
        }
        String sid = claims.get("sid") instanceof String s ? s : null;
        String sub = claims.get("sub") instanceof String s ? s : null;
        if (sid == null && sub == null) {
            throw new InvalidTokenException("neither sid nor sub");
        }
        int ended = 0;
        for (Iterator<Session> it = sessions.values().iterator(); it.hasNext(); ) {
            Session session = it.next();
            boolean sameIssuer = session.issuer().equals(config.issuer());
            boolean match = sid != null ? sid.equals(session.providerSid()) : sub.equals(session.account().subject());
            if (sameIssuer && match) {
                it.remove();
                ended++;
            }
        }
        return ended;
    }

    /** The names of the query parameters in an address, in order: enough to show what a redirect carries without its values. */
    static List<String> parameterNames(String url) {
        List<String> names = new ArrayList<>();
        int q = url.indexOf('?');
        if (q < 0) {
            return names;
        }
        for (String pair : url.substring(q + 1).split("&")) {
            names.add(pair.substring(0, pair.indexOf('=') < 0 ? pair.length() : pair.indexOf('=')));
        }
        return names;
    }

    private static boolean equal(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
