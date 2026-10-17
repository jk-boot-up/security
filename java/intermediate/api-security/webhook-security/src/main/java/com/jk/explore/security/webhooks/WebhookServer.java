package com.jk.explore.security.webhooks;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** The store's webhook endpoint on a real HTTP server on the loopback address, at POST /webhooks/payments, in front of a {@link Receiver}. */
final class WebhookServer implements AutoCloseable {

    static final int MAX_BODY = 4096;

    private final HttpServer server;

    WebhookServer(Receiver receiver) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/webhooks/payments", exchange -> handle(exchange, receiver));
        server.start();
    }

    private static void handle(HttpExchange exchange, Receiver receiver) throws IOException {
        Reply reply;
        if (!exchange.getRequestMethod().equals("POST")) {
            reply = new Reply(405, "refused: POST only");
        } else {
            // the raw bytes are kept exactly as they arrived: the signature covers these bytes, not a parsed copy
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readNBytes(MAX_BODY + 1);
            }
            reply = body.length > MAX_BODY ? new Reply(413, "refused: body too large")
                    : receiver.receive(body, exchange.getRequestHeaders().getFirst(Signatures.HEADER));
        }
        byte[] text = reply.message().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(reply.status(), text.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(text);
        }
    }

    URI endpoint() {
        String host = server.getAddress().getAddress().getHostAddress();
        String authority = host.contains(":") ? "[" + host + "]" : host;
        return URI.create("http://" + authority + ":" + server.getAddress().getPort() + "/webhooks/payments");
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
