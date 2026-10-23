package com.jk.explore.security.securitychaos;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * The five services a payment depends on, each on its own port: authorization, fraud scoring, the audit sink,
 * the rate-limit store and the revocation responder. Every answer carries a padding line, as a verbose bank
 * gateway would, so a bandwidth limit is visible.
 */
final class Backends implements AutoCloseable {

    static final String AUTHZ = "authorization";
    static final String FRAUD = "fraud";
    static final String AUDIT = "audit";
    static final String RATE = "ratelimit";
    static final String OCSP = "revocation";

    private static final String PAD = "x".repeat(4000);

    private final Map<String, HttpServer> servers = new java.util.LinkedHashMap<>();
    private final Set<String> auditReceived = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    Backends(String bindHost) throws IOException {
        servers.put(AUTHZ, serve(bindHost, q -> q.get("account").startsWith("frozen") ? "deny" : "allow"));
        servers.put(FRAUD, serve(bindHost, q -> "score=" + (Integer.parseInt(q.get("amount")) >= 5000 ? 90 : 10)));
        servers.put(RATE, serve(bindHost, q -> "count=" + counters.merge(q.get("account"), 1, Integer::sum)));
        servers.put(OCSP, serve(bindHost, q -> revoked.contains(q.get("serial")) ? "revoked" : "good"));
        servers.put(AUDIT, serveAudit(bindHost));
        revoked.add("rev-7");
    }

    int port(String name) {
        return servers.get(name).getAddress().getPort();
    }

    Set<String> names() {
        return servers.keySet();
    }

    int auditCount() {
        return auditReceived.size();
    }

    void revoke(String serial) {
        revoked.add(serial);
    }

    /** Forget the audit records, the counters and any late revocations. */
    void reset() {
        auditReceived.clear();
        counters.clear();
        revoked.clear();
        revoked.add("rev-7");
    }

    private static HttpServer serve(String host, Function<Map<String, String>, String> answer) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, 0), 0);
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            reply(ex, path.equals("/health") ? "ok" : answer.apply(query(ex.getRequestURI().getRawQuery())));
        });
        server.setExecutor(Executors.newCachedThreadPool(r -> daemon(r)));
        server.start();
        return server;
    }

    private HttpServer serveAudit(String host) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, 0), 0);
        server.createContext("/", ex -> {
            if (ex.getRequestURI().getPath().equals("/audit")) {
                auditReceived.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim());
                reply(ex, "stored");
            } else {
                reply(ex, "ok");
            }
        });
        server.setExecutor(Executors.newCachedThreadPool(r -> daemon(r)));
        server.start();
        return server;
    }

    private static Thread daemon(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }

    private static void reply(HttpExchange ex, String firstLine) throws IOException {
        byte[] body = (firstLine + "\n" + PAD).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new java.util.HashMap<>();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                out.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        return out;
    }

    @Override
    public void close() {
        servers.values().forEach(s -> s.stop(0));
    }
}
