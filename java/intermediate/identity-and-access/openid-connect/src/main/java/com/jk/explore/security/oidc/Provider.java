package com.jk.explore.security.oidc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An OpenID Connect provider in one process: the bank, or another site the store lets customers sign in with. It publishes its
 * metadata and keys, runs the authorization code flow, and signs ID tokens (for the store), access tokens (for its own API) and
 * back-channel logout tokens, all with Ed25519. Codes and session ids come from counters, so every run is the same.
 */
final class Provider {

    static final String API_AUDIENCE = "https://api.bank.example";
    static final String LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";
    static final long ID_TOKEN_SECONDS = 300;
    static final long ACCESS_TOKEN_SECONDS = 300;

    record User(String subject, String email, boolean emailVerified, String name) {
    }

    record AuthorizationRequest(String issuer, String clientId, String redirectUri, String scope, String state, String nonce) {
    }

    record AuthorizationResponse(String code, String state) {
    }

    /** The token endpoint's reply. The ID token is null when the request did not ask for the openid scope. */
    record TokenResponse(String idToken, String accessToken) {
    }

    private record Grant(String clientId, String subject, String scope, String nonce, long authTime, String sid) {
    }

    private final String issuer;
    private final SimClock clock;
    private final Map<String, byte[]> seeds = new LinkedHashMap<>();
    private String activeKid;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, String> clients = new HashMap<>();
    private final Map<String, Grant> codes = new HashMap<>();
    private int counter;

    Provider(String issuer, SimClock clock) {
        this.issuer = issuer;
        this.clock = clock;
    }

    String issuer() {
        return issuer;
    }

    Provider addKey(String kid, byte[] seed) {
        seeds.put(kid, seed.clone());
        if (activeKid == null) {
            activeKid = kid;
        }
        return this;
    }

    void signWith(String kid) {
        if (!seeds.containsKey(kid)) {
            throw new IllegalArgumentException("no such key");
        }
        activeKid = kid;
    }

    void retireKey(String kid) {
        seeds.remove(kid);
    }

    Provider addUser(User user) {
        users.put(user.subject(), user);
        return this;
    }

    Provider addClient(String clientId, String redirectUri) {
        clients.put(clientId, redirectUri);
        return this;
    }

    /** The discovery document (OpenID Connect Discovery 1.0, section 3). */
    String metadata() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issuer", issuer);
        m.put("authorization_endpoint", issuer + "/authorize");
        m.put("token_endpoint", issuer + "/token");
        m.put("userinfo_endpoint", issuer + "/userinfo");
        m.put("jwks_uri", issuer + "/jwks.json");
        m.put("end_session_endpoint", issuer + "/logout");
        m.put("response_types_supported", List.of("code"));
        m.put("subject_types_supported", List.of("public"));
        m.put("id_token_signing_alg_values_supported", List.of("EdDSA"));
        m.put("code_challenge_methods_supported", List.of("S256"));
        m.put("backchannel_logout_supported", Boolean.TRUE);
        m.put("backchannel_logout_session_supported", Boolean.TRUE);
        return Json.write(m);
    }

    /** The public keys, as a JWKS document. Private keys never leave the provider. */
    String jwks() {
        List<Object> keys = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : seeds.entrySet()) {
            KeyPair pair = Ed25519.fromSeed(entry.getValue());
            Map<String, Object> jwk = new LinkedHashMap<>();
            jwk.put("kty", "OKP");
            jwk.put("crv", "Ed25519");
            jwk.put("kid", entry.getKey());
            jwk.put("use", "sig");
            jwk.put("alg", "EdDSA");
            jwk.put("x", Base64Url.encode(Ed25519.encode(pair.getPublic())));
            keys.add(jwk);
        }
        return Json.write(Map.of("keys", keys));
    }

    void publish(Web web) {
        web.serve(Discovery.metadataUrl(issuer), this::metadata);
        web.serve(issuer + "/jwks.json", this::jwks);
    }

    /** The user has signed in at the provider and agreed; the browser is sent back with a code. */
    AuthorizationResponse authorize(AuthorizationRequest request, String subject) throws InvalidTokenException {
        String registered = clients.get(request.clientId());
        if (registered == null || !registered.equals(request.redirectUri())) {
            throw new InvalidTokenException("unknown client or redirect address");
        }
        if (!users.containsKey(subject)) {
            throw new InvalidTokenException("unknown user");
        }
        counter++;
        String code = "code-" + counter;
        codes.put(code, new Grant(request.clientId(), subject, request.scope(), request.nonce(), clock.now(), "sid-" + counter));
        return new AuthorizationResponse(code, request.state());
    }

    /** The token endpoint. A code works once. */
    TokenResponse token(String code, String clientId) throws InvalidTokenException {
        Grant grant = codes.remove(code);
        if (grant == null || !grant.clientId().equals(clientId)) {
            throw new InvalidTokenException("the code is not valid");
        }
        String access = sign("at+jwt", accessClaims(grant.subject(), grant.clientId(), grant.scope()));
        boolean openid = List.of(grant.scope().split(" ")).contains("openid");
        String id = openid ? sign("JWT", idClaims(grant)) : null;
        return new TokenResponse(id, access);
    }

    private Map<String, Object> idClaims(Grant grant) {
        User user = users.get(grant.subject());
        long now = clock.now();
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("iss", issuer);
        c.put("sub", user.subject());
        c.put("aud", grant.clientId());
        c.put("exp", now + ID_TOKEN_SECONDS);
        c.put("iat", now);
        c.put("auth_time", grant.authTime());
        c.put("nonce", grant.nonce());
        c.put("acr", "urn:example:loa:2");
        c.put("amr", List.of("pwd", "otp"));
        c.put("sid", grant.sid());
        c.put("email", user.email());
        c.put("email_verified", user.emailVerified());
        return c;
    }

    Map<String, Object> accessClaims(String subject, String clientId, String scope) {
        long now = clock.now();
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("iss", issuer);
        c.put("sub", subject);
        c.put("aud", API_AUDIENCE);
        c.put("client_id", clientId);
        c.put("scope", scope);
        c.put("iat", now);
        c.put("exp", now + ACCESS_TOKEN_SECONDS);
        c.put("jti", "at-" + (++counter));
        return c;
    }

    /** A back-channel logout token (OpenID Connect Back-Channel Logout 1.0): an events claim, a session id, and never a nonce. */
    String logoutToken(String clientId, String subject, String sid) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("iss", issuer);
        c.put("aud", clientId);
        c.put("iat", clock.now());
        c.put("jti", "lt-" + (++counter));
        c.put("events", Map.of(LOGOUT_EVENT, Map.of()));
        c.put("sub", subject);
        c.put("sid", sid);
        return sign("logout+jwt", c);
    }

    /** The userinfo endpoint: claims about the user, for a valid access token only. */
    Map<String, Object> userinfo(String bearer) throws InvalidTokenException {
        Jws.Parts parts = Jws.parse(bearer);
        if (!"EdDSA".equals(parts.header().get("alg")) || !"at+jwt".equals(parts.header().get("typ"))) {
            throw new InvalidTokenException("not an access token");
        }
        byte[] seed = parts.header().get("kid") instanceof String kid ? seeds.get(kid) : null;
        if (seed == null || !Ed25519.verify(Ed25519.fromSeed(seed).getPublic(), parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("signature does not match");
        }
        Map<String, Object> claims = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));
        if (!API_AUDIENCE.equals(claims.get("aud")) || !issuer.equals(claims.get("iss"))) {
            throw new InvalidTokenException("not an access token for this provider");
        }
        if (!(claims.get("exp") instanceof Long exp) || clock.now() >= exp) {
            throw new InvalidTokenException("expired");
        }
        if (!(claims.get("scope") instanceof String scope) || !List.of(scope.split(" ")).contains("openid")) {
            throw new InvalidTokenException("the token has no openid scope");
        }
        User user = users.get((String) claims.get("sub"));
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("sub", user.subject());
        info.put("name", user.name());
        info.put("email", user.email());
        info.put("email_verified", user.emailVerified());
        return info;
    }

    /** Signs any claims with the active key. Package-private so that the tests and the demo can make tokens with wrong claims. */
    String sign(String typ, Map<String, Object> claims) {
        return signWith(activeKid, seeds.get(activeKid), typ, claims);
    }

    static String signWith(String kid, byte[] seed, String typ, Map<String, Object> claims) {
        PrivateKey key = Ed25519.privateKey(seed);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "EdDSA");
        header.put("typ", typ);
        header.put("kid", kid);
        return Jws.sign(header, Json.write(claims).getBytes(StandardCharsets.UTF_8), key);
    }

    /** The claims an ID token would carry for this user, client and nonce: a starting point for tokens with one claim changed. */
    Map<String, Object> sampleIdClaims(String subject, String clientId, String nonce) {
        return idClaims(new Grant(clientId, subject, "openid", nonce, clock.now(), "sid-sample"));
    }
}
