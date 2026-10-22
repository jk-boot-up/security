package com.jk.explore.security.failclosedspring;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** The fraud-check service: a real HTTP server on the loopback interface that can be made to misbehave. Unknown merchants are risky. */
final class FraudServer implements AutoCloseable {

    enum Mode { UP, ERROR, SLOW, GARBAGE, DROP }

    private final HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile Mode mode = Mode.UP;

    FraudServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());   // a slow answer must not hold up the next request
        server.createContext("/check", this::handle);
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void mode(Mode mode) {
        this.mode = mode;
    }

    /** How many requests have reached the service, which is what a long outage costs it. */
    int calls() {
        return calls.get();
    }

    private void handle(HttpExchange exchange) throws IOException {
        calls.incrementAndGet();
        String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        switch (mode) {
            case UP -> reply(exchange, 200, "application/json", request.contains("unknown-electronics") ? "{\"risk\":\"high\"}" : "{\"risk\":\"low\"}");
            case ERROR -> reply(exchange, 500, "application/json", "{\"error\":\"fraud engine crashed\"}");
            case GARBAGE -> reply(exchange, 200, "text/html", "<html>maintenance page</html>");
            case SLOW -> {
                try {
                    Thread.sleep(2000);
                    reply(exchange, 200, "application/json", "{\"risk\":\"low\"}");
                } catch (InterruptedException | IOException e) {
                    exchange.close();
                }
            }
            case DROP -> exchange.close();
        }
    }

    private static void reply(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
