package com.jk.explore.security.dast;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A small bank web application, run in this process. The first version has the usual flaws on purpose: a reflected page, a query built with a string,
 * a session cookie without flags, no security headers, and an admin area behind a login. The fixed version closes them.
 */
public final class BankApp implements AutoCloseable {

    private final HttpServer server;
    private final Connection db;
    private final boolean fixed;
    private final String session = UUID.randomUUID().toString();

    public BankApp(boolean fixed) throws IOException, SQLException {
        this.fixed = fixed;
        this.db = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (Statement s = db.createStatement()) {
            s.execute("CREATE TABLE accounts(id INT PRIMARY KEY, owner VARCHAR(40), balance INT)");
            s.execute("INSERT INTO accounts VALUES (1, 'Asha', 1200), (2, 'Ben', 300)");
        }
        // loopback only: Docker Desktop forwards host.docker.internal to it, and nothing else on the network can reach the deliberately weak site
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", ex -> respond(ex, 200, page("Bank", "<a href=\"/search?q=card\">Search</a> <a href=\"/account?id=1\">Account</a> <a href=\"/login\">Log in</a> <a href=\"/admin\">Admin</a>")));
        server.createContext("/search", ex -> respond(ex, 200, page("Search", "<p>Results for " + out(query(ex).getOrDefault("q", "")) + "</p>")));
        server.createContext("/account", this::account);
        server.createContext("/login", this::login);
        server.createContext("/admin", this::admin);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    /** The cookie value that the login hands out; a scanner that wants to be logged in sends it. */
    public String sessionToken() {
        return session;
    }

    public boolean fixed() {
        return fixed;
    }

    private String out(String text) {
        return fixed ? text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;") : text;
    }

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + title + "</title></head><body><h1>" + title + "</h1>" + body + "</body></html>";
    }

    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q != null) {
            for (String pair : q.split("&")) {
                String[] kv = pair.split("=", 2);
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }
        return map;
    }

    private void account(HttpExchange ex) throws IOException {
        String id = query(ex).getOrDefault("id", "1");
        StringBuilder body = new StringBuilder();
        try {
            ResultSet rs;
            if (fixed) {
                PreparedStatement ps = db.prepareStatement("SELECT owner, balance FROM accounts WHERE id = ?");
                ps.setInt(1, Integer.parseInt(id));
                rs = ps.executeQuery();
            } else {
                rs = db.createStatement().executeQuery("SELECT owner, balance FROM accounts WHERE id = " + id);
            }
            while (rs.next()) {
                body.append("<p>").append(out(rs.getString(1))).append(": ").append(rs.getInt(2)).append("</p>");
            }
            respond(ex, 200, page("Account", body.toString()));
        } catch (SQLException | NumberFormatException e) {
            respond(ex, fixed ? 400 : 500, page("Error", fixed ? "<p>Bad request</p>" : "<p>" + e.getMessage() + "</p>"));
        }
    }

    private void login(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            ex.getResponseHeaders().add("Set-Cookie", "session=" + session + (fixed ? "; HttpOnly; SameSite=Strict; Path=/" : "; Path=/"));
            ex.getResponseHeaders().add("Location", "/admin");
            respond(ex, 302, "");
        } else {
            respond(ex, 200, page("Log in", "<form method=\"post\" action=\"/login\"><input name=\"user\"><input name=\"password\" type=\"password\"><button>Log in</button></form>"));
        }
    }

    private void admin(HttpExchange ex) throws IOException {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null || !cookie.contains("session=" + session)) {
            ex.getResponseHeaders().add("Location", "/login");
            respond(ex, 302, "");
            return;
        }
        if (ex.getRequestURI().getPath().equals("/admin/report")) {
            respond(ex, 200, page("Report", "<h2>" + out(query(ex).getOrDefault("title", "Monthly")) + "</h2>"));
        } else {
            respond(ex, 200, page("Admin", "<a href=\"/admin/report?title=Monthly\">Monthly report</a>"));
        }
    }

    private void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        if (fixed) {
            ex.getResponseHeaders().add("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; form-action 'self'");
            ex.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.getResponseHeaders().add("Cross-Origin-Resource-Policy", "same-origin");
            ex.getResponseHeaders().add("Cross-Origin-Opener-Policy", "same-origin");
            ex.getResponseHeaders().add("Cross-Origin-Embedder-Policy", "require-corp");
            ex.getResponseHeaders().add("Permissions-Policy", "geolocation=()");
        }
        ex.sendResponseHeaders(status, status == 302 ? -1 : bytes.length);
        if (status != 302) {
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }

    @Override
    public void close() throws SQLException {
        server.stop(0);
        db.close();
    }
}
