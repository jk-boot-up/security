package com.jk.explore.security.csrf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The bank on a real HTTP server, on the loopback address and a free port. */
final class BankServer implements AutoCloseable {

    private final Bank bank;
    private final HttpServer server;

    BankServer(Bank bank) throws IOException {
        this.bank = bank;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::serve);
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    Bank bank() {
        return bank;
    }

    private void serve(HttpExchange exchange) throws IOException {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> h : exchange.getRequestHeaders().entrySet()) {
            headers.put(h.getKey().toLowerCase(Locale.ROOT), String.join("; ", h.getValue()));
        }
        String fields = "GET".equals(exchange.getRequestMethod())
                ? exchange.getRequestURI().getRawQuery()
                : new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Response response = bank.handle(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), headers, parse(fields)));
        response.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    static Map<String, String> parse(String fields) {
        Map<String, String> form = new HashMap<>();
        if (fields == null || fields.isEmpty()) {
            return form;
        }
        for (String pair : fields.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                form.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
