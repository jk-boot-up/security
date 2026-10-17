package com.jk.explore.security.https;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * The online store's web server, on this machine only: an HTTPS port and a plain HTTP port. A "strong" shop redirects HTTP to HTTPS,
 * sends HSTS and the other security headers, and marks its session cookie Secure, HttpOnly and SameSite. A "weak" shop serves
 * everything on both ports with none of that.
 */
final class ShopServer implements AutoCloseable {

    private final HttpsServer https;
    private final HttpServer plain;
    private final boolean strong;

    ShopServer(SSLContext context, boolean strong, String... protocols) throws IOException {
        this.strong = strong;
        https = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(HttpsParameters parameters) {
                SSLParameters ssl = getSSLContext().getDefaultSSLParameters();
                ssl.setProtocols(protocols);
                parameters.setSSLParameters(ssl);
            }
        });
        plain = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        https.createContext("/", exchange -> handle(exchange, true));
        plain.createContext("/", exchange -> handle(exchange, false));
        https.start();
        plain.start();
    }

    int httpsPort() {
        return https.getAddress().getPort();
    }

    int plainPort() {
        return plain.getAddress().getPort();
    }

    private void handle(HttpExchange exchange, boolean secure) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!secure && strong) {
            exchange.getResponseHeaders().add("Location", "https://" + TestCertificates.HOST + ":" + httpsPort() + path);
            respond(exchange, 301, "");
            return;
        }
        if (secure && strong) {
            exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'self'");
            exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer");
        }
        switch (path) {
            case "/login" -> {
                exchange.getResponseHeaders().add("Set-Cookie", strong ? "session=abc123; Secure; HttpOnly; SameSite=Lax; Path=/" : "session=abc123; Path=/");
                respond(exchange, 200, "logged in");
            }
            case "/whoami" -> respond(exchange, 200, exchange.getRequestHeaders().containsKey("Cookie") ? "cookie received" : "no cookie");
            default -> respond(exchange, 200, "cart: 2 items");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    @Override
    public void close() {
        https.stop(0);
        plain.stop(0);
    }
}
