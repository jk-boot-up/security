package com.jk.explore.security.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** A real HTTP server and client on the loopback interface, so the audit runs on headers that really crossed a socket. */
final class LoopbackExchange {

    record Result(int status, String body, Map<String, String> headers) {
    }

    static final Map<String, String> HARDENED = new LinkedHashMap<>();

    static {
        HARDENED.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        HARDENED.put("Content-Security-Policy", "default-src 'self'; script-src 'self'");
        HARDENED.put("X-Content-Type-Options", "nosniff");
        HARDENED.put("X-Frame-Options", "DENY");
        HARDENED.put("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    private LoopbackExchange() {
    }

    /** Starts a server that answers /order with the given extra headers, fetches it once and stops the server. */
    static Result fetch(Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/order", exchange -> {
            byte[] body = "order A-1001 received".getBytes(StandardCharsets.UTF_8);
            extraHeaders.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            URI uri = URI.create("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort() + "/order");
            java.net.http.HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).GET().build(), BodyHandlers.ofString());
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((name, values) -> headers.put(name, String.join(", ", values)));
            return new Result(response.statusCode(), response.body(), headers);
        } finally {
            server.stop(0);
        }
    }
}
