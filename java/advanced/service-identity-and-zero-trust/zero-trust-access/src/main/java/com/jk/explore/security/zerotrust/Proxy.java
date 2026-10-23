package com.jk.explore.security.zerotrust;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * An identity-aware proxy in front of the ledger. Every request gets the same three steps and exactly one audit record:
 * authenticate (a JWT checked against the issuer's keys), authorize (the policy engine decides), and only then forward.
 */
final class Proxy implements AutoCloseable {

    /** What happened to one request. */
    private record Outcome(int status, String reason, String user, String body, boolean allowed) {
    }

    private final HttpServer server;
    private final Map<String, PublicKey> keys;
    private final String issuer;
    private final String audience;
    private final Decider decider;
    private final URI ledger;
    private final String mark;
    private final AuditLog audit;
    private final Clock clock;
    private final HttpClient http = HttpClient.newHttpClient();
    private volatile IntSupplier hour = () -> 10;
    private volatile Predicate<String> sessionCheck;

    Proxy(Map<String, PublicKey> keys, String issuer, String audience, Decider decider, URI ledger, String mark, AuditLog audit, Clock clock) throws IOException {
        this.keys = keys;
        this.issuer = issuer;
        this.audience = audience;
        this.decider = decider;
        this.ledger = ledger;
        this.mark = mark;
        this.audit = audit;
        this.clock = clock;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    /** The hour of day the policy sees. A real proxy reads its clock; the demo sets it so that its output does not depend on the time it runs. */
    void setHour(IntSupplier hour) {
        this.hour = hour;
    }

    /** Switches on (or, with null, off) a check at the issuer that the session behind the token is still alive. */
    void setSessionCheck(Predicate<String> check) {
        this.sessionCheck = check;
    }

    private void handle(HttpExchange x) throws IOException {
        byte[] body = x.getRequestBody().readAllBytes();
        String method = x.getRequestMethod();
        String path = x.getRequestURI().getPath();
        Outcome o;
        try {
            o = process(x, method, path, body);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // when the decision cannot be made the answer is no
            o = new Outcome(503, "decision unavailable", "-", "no decision", false);
        }
        audit.record(o.allowed() ? "allow" : "deny", o.status(), o.reason(), o.user(), method, path);
        Ledger.reply(x, o.status(), o.body());
    }

    private Outcome process(HttpExchange x, String method, String path, byte[] body) throws Exception {
        String header = x.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() < 8) {
            return new Outcome(401, "no token", "-", "authentication required", false);
        }
        String token = header.substring(7);
        Jwt.Result jwt = Jwt.verify(token, keys, issuer, audience, clock.instant());
        if (!jwt.valid()) {
            return new Outcome(401, jwt.reason(), jwt.user(), "authentication failed", false);
        }
        Predicate<String> check = sessionCheck;
        if (check != null && !check.test(token)) {
            return new Outcome(401, "session ended", jwt.user(), "authentication failed", false);
        }
        Map<String, Object> input = new HashMap<>();
        input.put("user", jwt.user());
        input.put("roles", roles(jwt.claims()));
        input.put("device_compliant", jwt.claims().get("compliant"));
        input.put("method", method);
        input.put("path", path);
        input.put("hour", hour.getAsInt());
        Decider.Decision d = decider.decide(input);
        if (!d.allow()) {
            return new Outcome(403, String.join(" + ", d.reasons()), jwt.user(), "forbidden", false);
        }
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(ledger.resolve(path)).header("X-Proxy-Mark", mark)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.ofString());
        return new Outcome(res.statusCode(), "allowed", jwt.user(), res.body(), true);
    }

    private static List<?> roles(Map<String, Object> claims) {
        return claims.get("realm_access") instanceof Map<?, ?> m && m.get("roles") instanceof List<?> l ? l : List.of();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
