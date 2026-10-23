package com.jk.explore.security.zerotrust;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The bank's internal ledger API. In perimeter mode it answers anyone who can reach it. Behind a proxy it answers only a request that carries
 * the proxy's mark (a random value made at run time); in production that link would be mutual TLS, as in the mutual-tls-between-services project.
 */
final class Ledger implements AutoCloseable {

    private final HttpServer server;
    private final String mark;
    private final AtomicInteger executed = new AtomicInteger();
    private final AtomicInteger transfers = new AtomicInteger();

    private Ledger(String mark) throws IOException {
        this.mark = mark;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    /** The perimeter model: no identity is asked for. */
    static Ledger open() throws IOException {
        return new Ledger(null);
    }

    /** The zero trust model: only the proxy gets an answer. */
    static Ledger behindProxy(String mark) throws IOException {
        return new Ledger(mark);
    }

    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    /** Requests that reached the ledger's own code (and did something). */
    int executed() {
        return executed.get();
    }

    int transfers() {
        return transfers.get();
    }

    private void handle(HttpExchange x) throws IOException {
        x.getRequestBody().readAllBytes();
        if (mark != null && !mark.equals(x.getRequestHeaders().getFirst("X-Proxy-Mark"))) {
            reply(x, 403, "only the proxy may call the ledger");
            return;
        }
        String route = x.getRequestMethod() + " " + x.getRequestURI().getPath();
        if (route.matches("GET /accounts/[0-9]+/balance")) {
            executed.incrementAndGet();
            reply(x, 200, "{\"balance\": 1250000}");
        } else if (route.equals("POST /transfers")) {
            executed.incrementAndGet();
            transfers.incrementAndGet();
            reply(x, 200, "{\"transfer\": \"accepted\"}");
        } else if (route.equals("GET /audit/entries")) {
            executed.incrementAndGet();
            reply(x, 200, "{\"entries\": 42}");
        } else if (route.equals("POST /admin/freeze")) {
            executed.incrementAndGet();
            reply(x, 200, "{\"freeze\": \"done\"}");
        } else {
            reply(x, 404, "no such route");
        }
    }

    static void reply(HttpExchange x, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        x.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            x.getResponseBody().write(b);
        }
        x.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
