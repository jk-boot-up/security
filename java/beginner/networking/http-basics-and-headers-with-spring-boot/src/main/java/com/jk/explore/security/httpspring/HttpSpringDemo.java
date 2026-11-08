package com.jk.explore.security.httpspring;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
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

    /** The properties that harden the store, next to its filter chain in {@link Shop.Hardened}. */
    static final List<String> HARDENED_PROPERTIES = List.of(
            "server.max-http-request-header-size=1KB",
            "server.servlet.session.cookie.http-only=true",
            "server.servlet.session.cookie.secure=true",
            "server.servlet.session.cookie.same-site=strict",
            "server.forward-headers-strategy=native");

    static Running hardened() {
        return start(Shop.Hardened.class, HARDENED_PROPERTIES.toArray(String[]::new));
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

    private static final List<String> FIVE = List.of("Strict-Transport-Security", "Content-Security-Policy",
            "X-Content-Type-Options", "X-Frame-Options", "Referrer-Policy");

    /** One point per header, added up in the open: "0 + 1 + 1 + 1 + 1 = 4 of 5". */
    private static String points(Wire.Reply reply) {
        List<HeaderAudit.Finding> findings = HeaderAudit.audit(reply.firstValues());
        StringBuilder sum = new StringBuilder();
        for (HeaderAudit.Finding finding : findings) {
            sum.append(sum.length() == 0 ? "" : " + ").append(finding.passed() ? 1 : 0);
        }
        return sum + " = " + HeaderAudit.score(findings) + " of 5";
    }

    /** The five header names, padded, each with its value or "missing"; a policy is shown one directive per line. */
    private static void fiveHeaders(StringBuilder out, Wire.Reply reply) {
        for (String name : FIVE) {
            List<String> values = reply.all(name);
            String value = values.isEmpty() ? "missing" : values.get(0);
            String[] parts = name.equals("Content-Security-Policy") ? value.split("; ") : new String[] {value};
            for (int i = 0; i < parts.length; i++) {
                line(out, "    " + String.format("%-27s", i == 0 ? name : "") + parts[i] + (i < parts.length - 1 ? ";" : ""));
            }
        }
    }

    private static long headerLimit(Running store) {
        return store.context().getBean(ServerProperties.class).getMaxHttpRequestHeaderSize().toBytes();
    }

    private static void act1Headers(StringBuilder out, Running bare, Running hardened) throws IOException, InterruptedException {
        line(out, "ONE. The same page, with and without Spring Security's header writers.");
        line(out, "  THE REQUEST (the same to both stores, over plain HTTP):");
        line(out, "    GET /products HTTP/1.1");
        Wire.Reply plain = Wire.get(bare.port(), "/products", Map.of());
        Wire.Reply strong = Wire.get(hardened.port(), "/products", Map.of());
        line(out, "  THE RESPONSE OF THE BARE STORE (status " + plain.status() + "):");
        fiveHeaders(out, plain);
        line(out, "  THE RESPONSE OF THE HARDENED STORE (status " + strong.status() + "):");
        fiveHeaders(out, strong);
        line(out, "  THE AUDIT (one point per header, in the order above):");
        line(out, "    bare store       " + points(plain));
        line(out, "    hardened store   " + points(strong));
        line(out, "  Strict-Transport-Security is left out on plain HTTP on purpose:");
        line(out, "  a browser must ignore it there, so sending it would only mislead");
    }

    private static void act2BehindAProxy(StringBuilder out, Running hardened) throws IOException, InterruptedException {
        line(out, "TWO. The same page behind a proxy that ends HTTPS.");
        line(out, "  THE REQUEST (as the proxy passes it on to the hardened store):");
        line(out, "    GET /products HTTP/1.1");
        line(out, "    X-Forwarded-Proto: https      <- added by the proxy");
        Wire.Reply reply = Wire.get(hardened.port(), "/products", Map.of("X-Forwarded-Proto", "https"));
        line(out, "  THE RESPONSE (status " + reply.status() + "):");
        fiveHeaders(out, reply);
        String hsts = reply.all("Strict-Transport-Security").get(0);
        long seconds = Long.parseLong(hsts.replaceAll(".*max-age=(\\d+).*", "$1"));
        line(out, "  THE AUDIT:");
        line(out, "    hardened store   " + points(reply));
        line(out, "    max-age " + seconds + " / 86400 seconds a day = " + seconds / 86_400 + " days");
        line(out, "  trusting X-Forwarded-Proto is safe only because the store");
        line(out, "  listens on the loopback interface and only the proxy reaches it");
    }

    /** The Set-Cookie header with the random session id hidden, so the output is the same on every run. */
    private static String withoutId(String setCookie) {
        int equals = setCookie.indexOf('=');
        int end = setCookie.indexOf(';');
        return setCookie.substring(0, equals + 1) + "<" + (end - equals - 1) + " random characters>" + setCookie.substring(end);
    }

    private static void act3Cookies(StringBuilder out, Running bare, Running hardened) throws IOException, InterruptedException {
        line(out, "THREE. The cookie that carries the cart's session.");
        Map<String, Running> stores = new java.util.LinkedHashMap<>();
        stores.put("default ", bare);
        stores.put("hardened", hardened);
        Map<String, Cookie> cookies = new java.util.LinkedHashMap<>();
        line(out, "  THE COOKIES (the Set-Cookie header each store sends for /cart):");
        for (Map.Entry<String, Running> store : stores.entrySet()) {
            String header = Wire.get(store.getValue().port(), "/cart", Map.of()).all("Set-Cookie").get(0);
            cookies.put(store.getKey(), Cookie.parse(header));
            String shown = withoutId(header);
            int split = shown.indexOf("; ");
            line(out, "    " + store.getKey() + "  " + shown.substring(0, split));
            line(out, "              " + shown.substring(split + 2));
        }
        line(out, "  THE VERDICT (what is wrong, if it carries a login):");
        for (Map.Entry<String, Cookie> cookie : cookies.entrySet()) {
            int n = cookie.getValue().problems().size();
            line(out, "    " + cookie.getKey() + "  " + n + (n == 1 ? " problem" : " problems"));
            for (String problem : cookie.getValue().problems()) {
                line(out, "      - " + problem);
            }
        }
        line(out, "  THE FIX (three properties of the hardened store):");
        HARDENED_PROPERTIES.stream().filter(p -> p.contains(".cookie.")).forEach(p -> line(out, "    " + p));
    }

    private static void act4Injection(StringBuilder out, Running bare) throws IOException, InterruptedException {
        line(out, "FOUR. A line break inside a header.");
        line(out, "  THE SEARCH TEXT (the store puts it in the X-Echo header):");
        line(out, "    normal   shoes");
        line(out, "    attack   shoes\\r\\nSet-Cookie: evil=1");
        Wire.Reply normal = Wire.get(bare.port(), "/echo?name=shoes", Map.of());
        Wire.Reply attack = Wire.get(bare.port(), "/echo?name=shoes%0d%0aSet-Cookie:%20evil=1", Map.of());
        line(out, "  THE RESPONSES:");
        line(out, "    normal   status " + normal.status() + ", X-Echo: " + String.join(",", normal.all("X-Echo")));
        line(out, "    attack   status " + attack.status() + ", X-Echo header: "
                + (attack.all("X-Echo").isEmpty() ? "none" : "present"));
        line(out, "             second Set-Cookie: " + (attack.all("Set-Cookie").isEmpty() ? "none" : "PRESENT  <- HEADER INJECTION"));
        line(out, "  THE REQUEST (raw bytes, with a space inside a header name):");
        line(out, "    Bad Header: x");
        int status = Wire.rawStatus(bare.port(), "GET /products HTTP/1.1\r\nHost: shop.example\r\nBad Header: x\r\n\r\n");
        line(out, "  THE RESPONSE:");
        line(out, "    status " + status);
        line(out, "  the container turns both into errors, and the code must still");
        line(out, "  never write customer text into a header unchecked");
    }

    private static void act5Limits(StringBuilder out, Running bare, Running hardened) throws IOException {
        line(out, "FIVE. Limits on the size of a request.");
        String padding = "X-Padding: " + "a".repeat(4000) + "\r\n";
        String big = "GET /products HTTP/1.1\r\nHost: shop.example\r\n" + padding + "Connection: close\r\n\r\n";
        line(out, "  THE REQUEST (one header made of 4000 letters a):");
        line(out, "    X-Padding: aaaa...aaaa\\r\\n   11 + 4000 + 2 = " + padding.length() + " bytes");
        line(out, "    the whole request             " + " ".repeat(14) + big.length() + " bytes");
        int accepted = Wire.rawStatus(bare.port(), big);
        int refused = Wire.rawStatus(hardened.port(), big);
        line(out, "  THE RESPONSES (each store has its own header limit):");
        line(out, "    default store    limit " + headerLimit(bare) + " bytes   status " + accepted);
        line(out, "    hardened store   limit " + headerLimit(hardened) + " bytes   status " + refused);
        line(out, "  a small limit stops one customer from making the server");
        line(out, "  hold megabytes, but it must stay high enough for the");
        line(out, "  real cookies of real customers");
    }
}
