package com.jk.explore.security.xss;

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
import java.util.List;

/** The product page served for real on the loopback interface, with a fresh nonce, a strict policy and an HttpOnly session cookie per response. */
final class ShopServer implements AutoCloseable {

    private final HttpServer server;

    ShopServer(List<Review> reviews) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/product", exchange -> {
            String nonce = ContentSecurityPolicy.newNonce();
            byte[] body = ReviewPage.safe(reviews, nonce).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().add("Content-Security-Policy", ContentSecurityPolicy.strict(nonce));
            exchange.getResponseHeaders().add("Set-Cookie", "session=demo-" + nonce.length() + "; Path=/; HttpOnly; Secure; SameSite=Lax");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    HttpResponse<String> get() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            InetSocketAddress at = server.getAddress();
            URI uri = URI.create("http://" + at.getHostString() + ":" + at.getPort() + "/product");
            return client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
