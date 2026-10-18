package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Map;

/**
 * The bank's API, a resource server. It checks every access token (signature, type, issuer, audience, expiry) and, when the token
 * is bound to a key, demands a DPoP proof from that key. Its answers never say more than the status and a short reason.
 */
final class BankApi {

    static final String ACCOUNTS = AuthServer.AUDIENCE + "/accounts";
    static final String TRANSFERS = AuthServer.AUDIENCE + "/transfers";

    private final SimClock clock;
    private final PublicKey issuerKey;
    private final DpopVerifier dpop;
    private int transfers;

    BankApi(SimClock clock, PublicKey issuerKey) {
        this.clock = clock;
        this.issuerKey = issuerKey;
        this.dpop = new DpopVerifier(clock);
    }

    int transfersMade() {
        return transfers;
    }

    Response handle(Request request) {
        String authorization = request.header("Authorization");
        if (authorization == null) {
            return Response.of(401, "no access token");
        }
        boolean dpopScheme = authorization.startsWith("DPoP ");
        if (!dpopScheme && !authorization.startsWith("Bearer ")) {
            return Response.of(401, "unknown authorization scheme");
        }
        String token = authorization.substring(authorization.indexOf(' ') + 1);
        Map<String, Object> claims;
        try {
            claims = verifyAccessToken(token);
        } catch (InvalidTokenException e) {
            return Response.of(401, e.getMessage());
        }
        if (claims.get("cnf") instanceof Map<?, ?> cnf && cnf.get("jkt") instanceof String boundTo) {
            if (!dpopScheme) {
                return Response.of(401, "this token is bound to a key: a DPoP proof is required");
            }
            try {
                String signedBy = dpop.verify(request.header("DPoP"), request.method(), request.url(), token);
                if (!Bytes.same(signedBy, boundTo)) {
                    return Response.of(401, "the proof's key is not the key the token is bound to");
                }
            } catch (InvalidTokenException e) {
                return Response.of(401, e.getMessage());
            }
        }
        String target = DpopVerifier.target(request.url());
        if (request.method().equals("GET") && target.equals(ACCOUNTS)) {
            return Response.of(200, "balance 1200.00");
        }
        if (request.method().equals("POST") && target.equals(TRANSFERS)) {
            transfers++;
            return Response.of(201, "transfer accepted");
        }
        return Response.of(404, "no such resource");
    }

    private Map<String, Object> verifyAccessToken(String token) throws InvalidTokenException {
        Jws.Parts parts = Jws.parse(token);
        if (!"EdDSA".equals(parts.header().get("alg")) || !"at+jwt".equals(parts.header().get("typ"))) {
            throw new InvalidTokenException("not an access token from this bank");
        }
        if (!Ed25519.verify(issuerKey, parts.signingInput(), parts.signature())) {
            throw new InvalidTokenException("the token's signature does not match");
        }
        Map<String, Object> claims = Json.parseObject(new String(parts.payload(), StandardCharsets.UTF_8));
        if (!AuthServer.ISSUER.equals(claims.get("iss")) || !AuthServer.AUDIENCE.equals(claims.get("aud"))) {
            throw new InvalidTokenException("the token is for another issuer or audience");
        }
        if (!(claims.get("exp") instanceof Long exp) || clock.now() >= exp) {
            throw new InvalidTokenException("the token has expired");
        }
        return claims;
    }
}
