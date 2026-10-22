package com.jk.explore.security.oidcspring;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A small provider on the loopback interface whose only job is to sign ID tokens with claims Keycloak would never issue, so that Spring Security's checks can be seen
 * refusing them. It serves its public key at /jwks.json, and it can serve a discovery document that says whatever the demo wants it to say.
 */
final class LocalProvider implements AutoCloseable {

    static final String ISSUER = "https://bank.example";
    static final String CLIENT = "store-web";

    private final HttpServer server;
    private final RSAKey key;
    private final Clock clock;
    private volatile String discovery = "{}";

    LocalProvider(Clock clock) throws IOException, JOSEException {
        this.clock = clock;
        this.key = new RSAKeyGenerator(2048).keyID("lab-key").generate();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/jwks.json", exchange -> reply(exchange, new JWKSet(key.toPublicJWK()).toString()));
        server.createContext("/realms/lab/.well-known/openid-configuration", exchange -> reply(exchange, discovery));
        server.start();
    }

    String base() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String jwksUri() {
        return base() + "/jwks.json";
    }

    /** The address a store would be configured with, whose discovery document says what {@link #serveDiscovery} was given. */
    String issuerAddress() {
        return base() + "/realms/lab";
    }

    void serveDiscovery(String issuerInDocument, String jwksUriInDocument) {
        discovery = "{\"issuer\":\"" + issuerInDocument + "\",\"authorization_endpoint\":\"" + base() + "/auth\",\"token_endpoint\":\"" + base() + "/token\","
                + "\"jwks_uri\":\"" + jwksUriInDocument + "\",\"response_types_supported\":[\"code\"],\"subject_types_supported\":[\"public\"],"
                + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}";
    }

    /** The claims of a good ID token, which each test then changes in one place. */
    JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer(ISSUER).subject("ana-0001").audience(CLIENT).issueTime(Date.from(clock.instant()))
                .expirationTime(Date.from(clock.instant().plus(Duration.ofMinutes(5))));
    }

    /** The claims of an ID token that expired the given number of seconds ago, issued five minutes before that. */
    JWTClaimsSet.Builder expiredSecondsAgo(long seconds) {
        return claims().issueTime(Date.from(clock.instant().minusSeconds(seconds + 300))).expirationTime(Date.from(clock.instant().minusSeconds(seconds)));
    }

    String sign(JWTClaimsSet claims) throws JOSEException {
        return sign(claims, key);
    }

    static String sign(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("lab-key").build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    String signWithAnotherKey(JWTClaimsSet claims) throws JOSEException {
        return sign(claims, new RSAKeyGenerator(2048).keyID("lab-key").generate());
    }

    String unsigned(JWTClaimsSet claims) {
        return new PlainJWT(claims).serialize();
    }

    static JWTClaimsSet.Builder with(JWTClaimsSet.Builder b, UnaryOperator<JWTClaimsSet.Builder> change) {
        return change.apply(b);
    }

    private static void reply(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    static List<String> kidsOf(String jwksJson) throws java.text.ParseException {
        return JWKSet.parse(jwksJson).getKeys().stream().filter(k -> "sig".equals(k.getKeyUse() == null ? null : k.getKeyUse().identifier())).map(k -> k.getKeyID()).toList();
    }
}
