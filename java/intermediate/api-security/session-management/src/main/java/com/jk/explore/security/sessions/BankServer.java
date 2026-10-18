package com.jk.explore.security.sessions;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** The bank's sign-in, account and sign-out pages on a real HTTP server on this machine's loopback address. */
final class BankServer implements AutoCloseable {

    private final HttpServer server;
    private final SessionManager sessions;

    BankServer(SessionManager sessions) throws IOException {
        this.sessions = sessions;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/login", this::login);
        server.createContext("/account", this::account);
        server.createContext("/logout", this::logout);
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    private void login(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            reply(exchange, 405, "POST only");
            return;
        }
        byte[] raw = exchange.getRequestBody().readNBytes(1024);
        Map<String, String> form = form(new String(raw, StandardCharsets.UTF_8));
        String user = form.getOrDefault("user", "");
        char[] password = form.getOrDefault("password", "").toCharArray();
        var id = sessions.signIn(sessionId(exchange), user, password);
        Arrays.fill(password, '\0');
        if (id.isEmpty()) {
            reply(exchange, 401, "wrong name or password");
            return;
        }
        exchange.getResponseHeaders().add("Set-Cookie", Cookies.sessionCookie(id.get()));
        reply(exchange, 200, "signed in");
    }

    private void account(HttpExchange exchange) throws IOException {
        Answer answer = sessions.account(sessionId(exchange));
        if (answer == Answer.ALLOWED) {
            reply(exchange, 200, "balance 1200.00");
        } else {
            reply(exchange, 401, answer.text());
        }
    }

    private void logout(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            reply(exchange, 405, "POST only");
            return;
        }
        sessions.signOut(sessionId(exchange));
        exchange.getResponseHeaders().add("Set-Cookie", Cookies.forget());
        reply(exchange, 200, "signed out");
    }

    private static String sessionId(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            String cookie = part.strip();
            if (cookie.startsWith(Cookies.NAME + "=")) {
                return cookie.substring(Cookies.NAME.length() + 1);
            }
        }
        return null;
    }

    private static Map<String, String> form(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                form.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    private static void reply(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
