package com.jk.explore.security.oauth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The bank's authorization server: the authorization endpoint the browser visits, and the token endpoint the client calls directly.
 * It follows RFC 6749 for the code flow, RFC 7636 for PKCE, and RFC 9700 for current practice: PKCE always, S256 only, exact redirect URIs,
 * single-use short-lived codes, refresh token rotation, and no implicit or password grant.
 */
final class AuthorizationServer {

    static final long CODE_LIFETIME = 60;
    static final long ACCESS_LIFETIME = 300;

    /** What the authorization endpoint does with a request: send the browser back to the client, or show an error page at the bank. */
    record AuthorizeResult(String redirectTo, String errorPage) {

        boolean redirected() {
            return redirectTo != null;
        }
    }

    /** What the token endpoint returns. */
    record Tokens(String accessToken, String refreshToken, long expiresIn, Set<String> scope) {
    }

    /** What the resource server learns about an access token (RFC 7662 introspection, in-process). */
    record Introspection(boolean active, String user, String clientId, Set<String> scope) {

        static final Introspection INACTIVE = new Introspection(false, null, null, Set.of());
    }

    private static final class CodeGrant {
        final String clientId;
        final String redirectUri;
        final String challenge;
        final String user;
        final Set<String> scope;
        final long expiresAt;
        boolean used;
        final List<String> issued = new ArrayList<>();

        CodeGrant(String clientId, String redirectUri, String challenge, String user, Set<String> scope, long expiresAt) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.challenge = challenge;
            this.user = user;
            this.scope = scope;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Access {
        final String clientId;
        final String user;
        final Set<String> scope;
        final long expiresAt;
        final String family;
        boolean revoked;

        Access(String clientId, String user, Set<String> scope, long expiresAt, String family) {
            this.clientId = clientId;
            this.user = user;
            this.scope = scope;
            this.expiresAt = expiresAt;
            this.family = family;
        }
    }

    private static final class Refresh {
        final String clientId;
        final String user;
        final Set<String> scope;
        final String family;
        boolean used;

        Refresh(String clientId, String user, Set<String> scope, String family) {
            this.clientId = clientId;
            this.user = user;
            this.scope = scope;
            this.family = family;
        }
    }

    private final SimClock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, ClientRegistration> clients = new HashMap<>();
    private final Map<String, CodeGrant> codes = new HashMap<>();
    private final Map<String, Access> accessTokens = new HashMap<>();
    private final Map<String, Refresh> refreshTokens = new HashMap<>();
    private final Set<String> revokedFamilies = new LinkedHashSet<>();

    AuthorizationServer(SimClock clock) {
        this.clock = clock;
    }

    AuthorizationServer register(ClientRegistration client) {
        clients.put(client.clientId(), client);
        return this;
    }

    Optional<ClientRegistration> client(String clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Pkce.base64Url(bytes);
    }

    static Set<String> scopes(String text) {
        Set<String> out = new TreeSet<>();
        if (text != null) {
            out.addAll(Arrays.asList(text.trim().split("\\s+")));
            out.remove("");
        }
        return out;
    }

    /**
     * The authorization endpoint, after the user has signed in at the bank and answered the consent question. Problems with the client or
     * its redirect URI are shown at the bank and never redirected, because a wrong redirect URI is exactly where an attacker wants the code.
     */
    AuthorizeResult authorize(Map<String, String> params, String user, boolean consented) {
        ClientRegistration client = clients.get(params.get("client_id"));
        if (client == null) {
            return new AuthorizeResult(null, "unknown client");
        }
        String redirectUri = params.get("redirect_uri");
        if (!client.registered(redirectUri)) {
            return new AuthorizeResult(null, "the redirect URI is not one this client registered");
        }
        String state = params.get("state");
        if (!"code".equals(params.get("response_type"))) {
            return redirectError(redirectUri, state, "unsupported_response_type", "only the authorization code flow is offered");
        }
        String challenge = params.get("code_challenge");
        if (challenge == null || challenge.isEmpty()) {
            return redirectError(redirectUri, state, "invalid_request", "PKCE is required: send a code_challenge");
        }
        if (!Pkce.S256.equals(params.get("code_challenge_method"))) {
            return redirectError(redirectUri, state, "invalid_request", "only the S256 code challenge method is accepted");
        }
        Set<String> scope = scopes(params.get("scope"));
        if (scope.isEmpty() || !client.allowedScopes().containsAll(scope)) {
            return redirectError(redirectUri, state, "invalid_scope", "the client may not ask for " + scope);
        }
        if (!consented) {
            return redirectError(redirectUri, state, "access_denied", "the customer said no");
        }
        String code = randomToken();
        codes.put(code, new CodeGrant(client.clientId(), redirectUri, challenge, user, scope, clock.now() + CODE_LIFETIME));
        Map<String, String> back = new LinkedHashMap<>();
        back.put("code", code);
        if (state != null) {
            back.put("state", state);
        }
        return new AuthorizeResult(redirectUri + "?" + Urls.form(back), null);
    }

    private static AuthorizeResult redirectError(String redirectUri, String state, String error, String description) {
        Map<String, String> back = new LinkedHashMap<>();
        back.put("error", error);
        back.put("error_description", description);
        if (state != null) {
            back.put("state", state);
        }
        return new AuthorizeResult(redirectUri + "?" + Urls.form(back), null);
    }

    /** The token endpoint. The client calls it directly, never through the browser. */
    Tokens token(Map<String, String> form) throws OAuthException {
        ClientRegistration client = authenticate(form);
        String grant = form.get("grant_type");
        if ("authorization_code".equals(grant)) {
            return redeemCode(client, form);
        }
        if ("refresh_token".equals(grant)) {
            return refresh(client, form.get("refresh_token"));
        }
        throw new OAuthException("unsupported_grant_type", "the " + grant + " grant is not offered");
    }

    private ClientRegistration authenticate(Map<String, String> form) throws OAuthException {
        ClientRegistration client = clients.get(form.get("client_id"));
        if (client == null) {
            throw new OAuthException("invalid_client", "unknown client");
        }
        if (client.confidential() && !client.secretMatches(form.get("client_secret"))) {
            throw new OAuthException("invalid_client", "client authentication failed");
        }
        return client;
    }

    private Tokens redeemCode(ClientRegistration client, Map<String, String> form) throws OAuthException {
        CodeGrant grant = codes.get(form.getOrDefault("code", ""));
        if (grant == null) {
            throw new OAuthException("invalid_grant", "unknown code");
        }
        if (grant.used) {
            grant.issued.forEach(this::revokeAccess);
            throw new OAuthException("invalid_grant", "the code was already used, so the tokens issued from it are revoked");
        }
        if (!grant.clientId.equals(client.clientId())) {
            throw new OAuthException("invalid_grant", "the code was issued to another client");
        }
        if (clock.now() > grant.expiresAt) {
            throw new OAuthException("invalid_grant", "the code has expired");
        }
        if (!grant.redirectUri.equals(form.get("redirect_uri"))) {
            throw new OAuthException("invalid_grant", "the redirect URI does not match the authorization request");
        }
        if (!Pkce.matches(grant.challenge, form.get("code_verifier"))) {
            throw new OAuthException("invalid_grant", "PKCE verification failed");
        }
        grant.used = true;
        Tokens tokens = issue(client.clientId(), grant.user, grant.scope, randomToken());
        grant.issued.add(tokens.accessToken());
        return tokens;
    }

    private Tokens refresh(ClientRegistration client, String token) throws OAuthException {
        Refresh old = refreshTokens.get(token == null ? "" : token);
        if (old == null || !old.clientId.equals(client.clientId())) {
            throw new OAuthException("invalid_grant", "unknown refresh token");
        }
        if (revokedFamilies.contains(old.family)) {
            throw new OAuthException("invalid_grant", "the refresh token was revoked");
        }
        if (old.used) {
            revokeFamily(old.family);
            throw new OAuthException("invalid_grant", "the refresh token was used twice, so every token in its family is revoked");
        }
        old.used = true;
        return issue(old.clientId, old.user, old.scope, old.family);
    }

    private Tokens issue(String clientId, String user, Set<String> scope, String family) {
        String access = randomToken();
        String refresh = randomToken();
        accessTokens.put(access, new Access(clientId, user, scope, clock.now() + ACCESS_LIFETIME, family));
        refreshTokens.put(refresh, new Refresh(clientId, user, scope, family));
        return new Tokens(access, refresh, ACCESS_LIFETIME, scope);
    }

    private void revokeAccess(String token) {
        Access access = accessTokens.get(token);
        if (access != null) {
            access.revoked = true;
            revokeFamily(access.family);
        }
    }

    private void revokeFamily(String family) {
        revokedFamilies.add(family);
        accessTokens.values().stream().filter(a -> a.family.equals(family)).forEach(a -> a.revoked = true);
    }

    /** What the bank's API asks before it serves a request. */
    Introspection introspect(String token) {
        Access access = token == null ? null : accessTokens.get(token);
        if (access == null || access.revoked || clock.now() >= access.expiresAt) {
            return Introspection.INACTIVE;
        }
        return new Introspection(true, access.user, access.clientId, access.scope);
    }
}
