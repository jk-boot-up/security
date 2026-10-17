package com.jk.explore.security.accesscontrol;

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

/** A store served over real HTTP on the loopback address, on a free port, and a client for it. */
final class HttpFront implements AutoCloseable {

    private final HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    HttpFront(Store store) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Reply reply = store.handle(Request.of(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), token, body));
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    /** Sends the request over the network and returns the status code. */
    int send(Request request) throws IOException, InterruptedException {
        URI uri = URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + request.path());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .method(request.method(), request.body().isEmpty()
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(request.body()));
        if (request.token() != null) {
            builder.header("Authorization", "Bearer " + request.token());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Override
    public void close() {
        server.stop(0);
        client.close();
    }
}
