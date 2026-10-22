package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** The bank's published key set at /jwks.json: a real HTTP server on the loopback interface that counts how often it is asked. */
final class JwksServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private volatile String body = "{\"keys\":[]}";

    JwksServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/jwks.json", this::handle);
        server.start();
    }

    URL url() {
        try {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks.json").toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Publishes these keys, public halves only. */
    void publish(List<? extends JWK> keys) {
        body = new JWKSet(keys.stream().map(JWK::toPublicJWK).map(JWK.class::cast).toList()).toString();
    }

    int requests() {
        return requests.get();
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
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
}
