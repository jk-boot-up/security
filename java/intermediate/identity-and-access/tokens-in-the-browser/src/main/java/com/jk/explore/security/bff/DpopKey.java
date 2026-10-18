package com.jk.explore.security.bff;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A client's DPoP key (RFC 9449): an Ed25519 key pair whose public half travels in every proof as a JWK. In a browser this is a
 * non-extractable WebCrypto key: scripts on the page can ask it to sign, but cannot read the private key and carry it away.
 */
final class DpopKey {

    private final KeyPair keys;
    private final SecureRandom random = new SecureRandom();

    DpopKey(KeyPair keys) {
        this.keys = keys;
    }

    /** The public key as a JWK (RFC 8037 section 2): an "OKP" key on the "Ed25519" curve, with its 32 bytes in "x". */
    Map<String, Object> jwk() {
        return jwk(keys.getPublic());
    }

    static Map<String, Object> jwk(PublicKey key) {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "OKP");
        jwk.put("crv", "Ed25519");
        jwk.put("x", Base64Url.encode(Ed25519.encode(key)));
        return jwk;
    }

    String thumbprint() {
        return thumbprint(jwk());
    }

    /**
     * The JWK thumbprint (RFC 7638): SHA-256 over the required members in lexicographic order with no spaces. For an OKP key
     * those are crv, kty and x, as RFC 8037 appendix A.3 shows.
     */
    static String thumbprint(Map<String, Object> jwk) {
        String canonical = "{\"crv\":\"" + jwk.get("crv") + "\",\"kty\":\"" + jwk.get("kty") + "\",\"x\":\"" + jwk.get("x") + "\"}";
        return Base64Url.encode(Bytes.sha256(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * A proof for one request: the method, the URL without query or fragment, the time, a unique id and, when an access token is
     * sent, its hash in "ath" (RFC 9449 section 4.2).
     */
    String proof(String method, String url, long issuedAt, String accessToken) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "dpop+jwt");
        header.put("alg", "EdDSA");
        header.put("jwk", jwk());
        Map<String, Object> claims = new LinkedHashMap<>();
        byte[] id = new byte[16];
        random.nextBytes(id);
        claims.put("jti", Base64Url.encode(id));
        claims.put("htm", method);
        claims.put("htu", DpopVerifier.target(url));
        claims.put("iat", issuedAt);
        if (accessToken != null) {
            claims.put("ath", Bytes.sha256Base64Url(accessToken));
        }
        return Jws.sign(header, Json.write(claims).getBytes(StandardCharsets.UTF_8), keys.getPrivate());
    }
}
