package com.jk.explore.security.oidc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** The provider's discovery document and key set served over real HTTP on this machine's loopback address, on a free port. */
final class LoopbackProvider implements AutoCloseable {

    private final HttpServer server;
    private final Provider provider;

    LoopbackProvider(SimClock clock, byte[] seed) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        provider = new Provider(issuer, clock).addKey("loop-1", seed);
        server.createContext("/.well-known/openid-configuration", e -> reply(e, provider.metadata()));
        server.createContext("/jwks.json", e -> reply(e, provider.jwks()));
        server.start();
    }

    Provider provider() {
        return provider;
    }

    private static void reply(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** Fetches a document with the JDK's HTTP client; anything but 200 is refused. */
    static String fetch(String url) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("status " + response.statusCode());
            }
            return response.body();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
