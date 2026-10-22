package com.jk.explore.security.ssrfspring;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small invented internet on the loopback address: a public image host, an attacker's host, the cloud metadata address, the shop's own
 * configuration service on localhost and an office admin page. Each is a real HTTP server on its own loopback port. This class is both the
 * name server for the invented names and the router that sends a connection for an invented address to the right server.
 */
final class Internet implements Dns, Router, AutoCloseable {

    static final String IMAGES = "203.0.113.10";
    static final String ATTACKER = "203.0.113.66";
    static final String METADATA = "169.254.169.254";
    static final String OFFICE = "10.0.0.5";

    private final Map<String, Integer> ports = new HashMap<>();
    private final Map<String, List<List<String>>> names = new HashMap<>();
    private final Map<String, AtomicInteger> asked = new HashMap<>();
    private final List<HttpServer> servers = new ArrayList<>();

    Internet() throws IOException {
        serve(IMAGES, ex -> reply(ex, 200, "image/png", "PNG-cat"));
        serve(ATTACKER, ex -> {
            switch (ex.getRequestURI().getPath()) {
                case "/cat.png" -> reply(ex, 200, "image/png", "PNG-attacker-cat");
                case "/redirect" -> {
                    ex.getResponseHeaders().add("Location", "http://" + METADATA + "/latest/meta-data/");
                    reply(ex, 302, "text/plain", "");
                }
                case "/big.png" -> reply(ex, 200, "image/png", "X".repeat(5000));
                case "/page.html" -> reply(ex, 200, "text/html", "<html>not an image</html>");
                case "/slow.png" -> {
                    sleep(3000);
                    reply(ex, 200, "image/png", "PNG-too-late");
                }
                default -> reply(ex, 404, "text/plain", "none");
            }
        });
        serve(METADATA, ex -> reply(ex, 200, "text/plain", "AccessKeyId=DEMO-NOT-A-REAL-KEY"));
        serve("127.0.0.1", ex -> reply(ex, 200, "text/plain", "DB_PASSWORD=demo-not-real"));
        serve(OFFICE, ex -> reply(ex, 200, "text/plain", "internal admin panel: 3 customers"));
        name("localhost", List.of("127.0.0.1")); // one answer, so that the first one is never ::1
        name("images.example", List.of(IMAGES));
        name("evil.example", List.of(ATTACKER));
        name("intranet.attacker.example", List.of(OFFICE));
        name("dual.attacker.example", List.of(ATTACKER, OFFICE));
        // a name server that lies: a public answer first, a private one every time after
        names.put("rebind.attacker.example", List.of(List.of(ATTACKER), List.of(OFFICE)));
    }

    private void name(String host, List<String> addresses) {
        names.put(host, List.of(addresses));
    }

    private void serve(String invented, HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", handler);
        // one thread per request: the slow handler must not hold up the next request to the same server
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        servers.add(server);
        ports.put(invented, server.getAddress().getPort());
    }

    private static void reply(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Invented names come from the table; everything else, including every odd spelling of an address, goes to the JDK's own parser. */
    @Override
    public List<InetAddress> resolve(String host) throws UnknownHostException {
        List<List<String>> answers = names.get(host.toLowerCase(java.util.Locale.ROOT));
        if (answers == null) {
            return Dns.SYSTEM.resolve(host);
        }
        int n = asked.computeIfAbsent(host.toLowerCase(java.util.Locale.ROOT), k -> new AtomicInteger()).getAndIncrement();
        List<InetAddress> out = new ArrayList<>();
        for (String address : answers.get(Math.min(n, answers.size() - 1))) {
            out.add(InetAddress.getByName(address));
        }
        return out;
    }

    /** Forgets how often each name was asked, so a test can start a story again. */
    void resetNameServer() {
        asked.clear();
    }

    @Override
    public InetSocketAddress route(InetAddress address, int port) {
        Integer local = ports.get(address.getHostAddress());
        if (local == null) {
            throw new IllegalStateException("nothing in the invented internet at " + address.getHostAddress());
        }
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), local);
    }

    @Override
    public void close() {
        servers.forEach(s -> {
            s.stop(0);
            ((java.util.concurrent.ExecutorService) s.getExecutor()).shutdownNow();
        });
    }
}
