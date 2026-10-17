package com.jk.explore.security.misconfig;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The online store on the loopback interface. What it exposes depends only on its settings. */
final class StoreServer implements AutoCloseable {

    /** The files in the store's static folder, including one that was never meant to be public. */
    static final Map<String, String> STATIC_FILES = new TreeMap<>(Map.of(
            "index.html", "<h1>Shop</h1>",
            "logo.png", "PNG",
            "backup-orders.csv", "order,customer,total\nA-1001,Ana,1200\n"));

    private static final Set<String> METHODS = Set.of("GET", "POST", "HEAD");

    private final StoreConfig config;
    private final HttpServer server;

    private StoreServer(StoreConfig config) throws IOException {
        this.config = config;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    /** Starts the store after the startup gate has accepted its settings. */
    static StoreServer start(StoreConfig config) throws IOException, InsecureConfigException {
        StartupGate.check(config);
        return new StoreServer(config);
    }

    int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!config.serverBanner().isEmpty()) {
                exchange.getResponseHeaders().set("Server", config.serverBanner());
            }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (method.equals("TRACE")) {
                if (config.traceAllowed()) {
                    StringBuilder echo = new StringBuilder("TRACE ").append(path).append('\n');
                    exchange.getRequestHeaders().forEach((k, v) -> echo.append(k).append(": ").append(String.join(",", v)).append('\n'));
                    send(exchange, 200, echo.toString());
                } else {
                    refuseMethod(exchange);
                }
                return;
            }
            if (!METHODS.contains(method)) {
                refuseMethod(exchange);
                return;
            }
            if (path.equals("/")) {
                send(exchange, 200, "<h1>Welcome to the shop</h1>");
            } else if (path.equals("/admin/login") && method.equals("POST")) {
                adminLogin(exchange);
            } else if (path.equals("/debug/config") && config.debugEndpoint()) {
                send(exchange, 200, "profile=" + config.profile() + "\nadmin.password=" + config.adminPassword()
                        + "\nerrors.detailed=" + config.detailedErrors() + "\n");
            } else if (path.equals("/static/")) {
                if (config.directoryListing()) {
                    send(exchange, 200, "Index of /static/\n" + String.join("\n", STATIC_FILES.keySet()) + "\n");
                } else {
                    send(exchange, 404, "not found");
                }
            } else if (path.startsWith("/static/") && STATIC_FILES.containsKey(path.substring(8))) {
                send(exchange, 200, STATIC_FILES.get(path.substring(8)));
            } else if (path.equals("/orders")) {
                order(exchange);
            } else {
                send(exchange, 404, "not found");
            }
        }
    }

    private void adminLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String given = "";
        for (String pair : body.split("&")) {
            if (pair.startsWith("password=")) {
                given = URLDecoder.decode(pair.substring(9), StandardCharsets.UTF_8);
            }
        }
        if (Secrets.same(given, config.adminPassword())) {
            send(exchange, 200, "welcome, admin");
        } else {
            send(exchange, 401, "wrong password");
        }
    }

    private void order(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String id = query != null && query.startsWith("id=") ? query.substring(3) : "";
        try {
            int number = Integer.parseInt(id);
            send(exchange, 200, "order " + number);
        } catch (NumberFormatException e) {
            if (config.detailedErrors()) {
                StringWriter trace = new StringWriter();
                e.printStackTrace(new PrintWriter(trace));
                send(exchange, 500, "<h1>Error</h1><pre>" + trace + "</pre>");
            } else {
                send(exchange, 500, "<h1>Something went wrong</h1><p>Please try again. Reference: E-500</p>");
            }
        }
    }

    private static void refuseMethod(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Allow", String.join(", ", List.of("GET", "HEAD", "POST")));
        send(exchange, 405, "method not allowed");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
