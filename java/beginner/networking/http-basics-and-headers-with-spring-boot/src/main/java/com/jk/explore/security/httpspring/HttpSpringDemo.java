package com.jk.explore.security.httpspring;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the online store story in five numbered acts against a real Spring Boot server on the loopback interface. */
public class HttpSpringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** A store started on a free port. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start(Class<?> configuration, String... properties) {
        String[] all = new String[properties.length + 4];
        System.arraycopy(properties, 0, all, 0, properties.length);
        all[properties.length] = "server.port=0";
        all[properties.length + 1] = "server.address=127.0.0.1";
        all[properties.length + 2] = "logging.level.root=OFF";
        all[properties.length + 3] = "spring.main.banner-mode=off";
        ConfigurableApplicationContext context = new SpringApplicationBuilder(configuration).properties(all).run();
        int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
        return new Running(context, port);
    }

    static Running bare() {
        return start(Shop.Bare.class);
    }

    static Running hardened() {
        return start(Shop.Hardened.class,
                "server.max-http-request-header-size=1KB",
                "server.servlet.session.cookie.same-site=strict",
                "server.servlet.session.cookie.secure=true",
                "server.servlet.session.cookie.http-only=true",
                "server.forward-headers-strategy=native");
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running bare = bare(); Running hardened = hardened()) {
            act1Headers(out, bare, hardened);
            act2BehindAProxy(out, hardened);
            act3Cookies(out, bare, hardened);
            act4Injection(out, bare);
            act5Limits(out, bare, hardened);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String audit(Wire.Reply reply) {
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(reply.firstValues());
        StringBuilder missing = new StringBuilder();
        for (HeaderAudit.Finding finding : findings) {
            if (!finding.passed()) {
                missing.append(missing.length() == 0 ? "" : ", ").append(finding.header());
            }
        }
        return HeaderAudit.score(findings) + " of 5" + (missing.length() == 0 ? "" : " (missing or weak: " + missing + ")");
    }

    private static void act1Headers(StringBuilder out, Running bare, Running hardened) throws IOException, InterruptedException {
        line(out, "ONE. The same page, with and without Spring Security's header writers.");
        line(out, "  a customer asks for /products over plain HTTP on this machine");
        line(out, "  the store with headers switched off scores " + audit(Wire.get(bare.port(), "/products", Map.of())));
        line(out, "  the hardened store scores " + audit(Wire.get(hardened.port(), "/products", Map.of())));
        line(out, "  the framework leaves out Strict-Transport-Security on plain HTTP on purpose: a browser must ignore it there, so sending it would only mislead");
    }

    private static void act2BehindAProxy(StringBuilder out, Running hardened) throws IOException, InterruptedException {
        line(out, "TWO. The same page behind a proxy that ends HTTPS.");
        Wire.Reply reply = Wire.get(hardened.port(), "/products", Map.of("X-Forwarded-Proto", "https"));
        line(out, "  the proxy adds X-Forwarded-Proto: https, and the hardened store scores " + audit(reply));
        line(out, "  Strict-Transport-Security: " + reply.all("Strict-Transport-Security").get(0));
        line(out, "  Content-Security-Policy: " + reply.all("Content-Security-Policy").get(0));
        line(out, "  this trust in a forwarded header is safe only because the store listens on the loopback interface and only the proxy can reach it");
    }

    private static void act3Cookies(StringBuilder out, Running bare, Running hardened) throws IOException, InterruptedException {
        line(out, "THREE. The cookie that carries the cart's session.");
        for (Map.Entry<String, Running> store : Map.of("default", bare, "hardened", hardened).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            Wire.Reply reply = Wire.get(store.getValue().port(), "/cart", Map.of());
            Cookie cookie = Cookie.parse(reply.all("Set-Cookie").get(0));
            line(out, "  " + store.getKey() + " store: cookie " + cookie.name() + ", problems: " + cookie.problems().size());
            for (String problem : cookie.problems()) {
                line(out, "    - " + problem);
            }
        }
        line(out, "  three properties turn the safe attributes on: cookie.http-only, cookie.secure and cookie.same-site");
    }

    private static void act4Injection(StringBuilder out, Running bare) throws IOException, InterruptedException {
        line(out, "FOUR. A line break inside a header.");
        Wire.Reply reply = Wire.get(bare.port(), "/echo?name=shoes%0d%0aSet-Cookie:%20evil=1", Map.of());
        line(out, "  the customer's search text, with a line break and a second header, is put in X-Echo");
        line(out, "  the answer contains a second header with the attacker's cookie: " + !reply.all("Set-Cookie").isEmpty()
                + (reply.all("Set-Cookie").isEmpty() ? "" : "  <- HEADER INJECTION"));
        line(out, "  the server refused to write a header with a line break: status " + reply.status() + ", and no X-Echo header at all: " + reply.all("X-Echo").isEmpty());
        int status = Wire.rawStatus(bare.port(), "GET /products HTTP/1.1\r\nHost: shop.example\r\nBad Header: x\r\n\r\n");
        line(out, "  a request with a space in a header name gets status " + status);
        line(out, "  the container turns the attack into an error, and the code must still never write customer text into a header unchecked");
    }

    private static void act5Limits(StringBuilder out, Running bare, Running hardened) throws IOException {
        line(out, "FIVE. Limits on the size of a request.");
        String big = "GET /products HTTP/1.1\r\nHost: shop.example\r\nX-Padding: " + "a".repeat(4000) + "\r\nConnection: close\r\n\r\n";
        int accepted = Wire.rawStatus(bare.port(), big);
        int refused = Wire.rawStatus(hardened.port(), big);
        line(out, "  a request with a 4000-character header: default store answers " + accepted + ", hardened store (limit 1KB) answers " + refused);
        line(out, "  a small limit keeps one customer from making the server read and hold megabytes, and the limit must still be high enough for real cookies");
    }
}
