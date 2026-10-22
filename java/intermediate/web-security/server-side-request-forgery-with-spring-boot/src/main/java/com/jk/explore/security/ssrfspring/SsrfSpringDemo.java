package com.jk.explore.security.ssrfspring;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the product image import story in five numbered acts against real Spring Boot servers and a small invented internet on loopback. */
public class SsrfSpringDemo {

    static {
        // read once by the JDK's HTTP client; the build also sets it on the command line
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    static final String HONEST = "http://images.example/cat.png";

    /** The attacker's ways to point the importer inside, with the names the plain project uses for them. */
    static final Map<String, String> ATTACKS = new LinkedHashMap<>();

    static {
        ATTACKS.put("metadata address", "http://169.254.169.254/latest/meta-data/");
        ATTACKS.put("localhost", "http://localhost/");
        ATTACKS.put("office admin page", "http://10.0.0.5/");
    }

    static final Map<String, String> DISGUISES = new LinkedHashMap<>();

    static {
        DISGUISES.put("metadata address", "http://169.254.169.254/latest/meta-data/");
        DISGUISES.put("decimal spelling", "http://2852039166/latest/meta-data/");
        DISGUISES.put("short loopback", "http://127.1/");
        DISGUISES.put("mapped IPv6 spelling", "http://[::ffff:a9fe:a9fe]/latest/meta-data/");
        DISGUISES.put("name that points inside", "http://intranet.attacker.example/");
        DISGUISES.put("redirect from outside", "http://evil.example/redirect");
    }

    /** A shop started on a free loopback port with one importer. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port, Internet internet) implements AutoCloseable {

        String importFrom(String address) throws IOException, InterruptedException {
            internet.resetNameServer();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/import"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("url=" + URLEncoder.encode(address, StandardCharsets.UTF_8))).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        @Override
        public void close() {
            context.close();
        }
    }

    static Running start(Internet internet, String importer) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ShopApp.class)
                .profiles(importer)
                .initializers(c -> c.getBeanFactory().registerSingleton("internet", internet))
                .properties("server.port=0", "server.address=127.0.0.1", "logging.level.root=OFF", "spring.main.banner-mode=off")
                .run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")), internet);
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only results are printed, never ports. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Internet internet = new Internet()) {
            try (Running shop = start(internet, "naive")) {
                act1And2Naive(out, shop);
            }
            try (Running shop = start(internet, "blocklist")) {
                act3Blocklist(out, shop);
            }
            try (Running shop = start(internet, "guarded")) {
                act4Guarded(out, shop);
            }
            act5CheckThenConnect(out, internet);
        }
        return out.toString();
    }

    private static void row(StringBuilder out, String label, String result) {
        out.append(String.format("%-31s -> %s%n", label, result));
    }

    private static void act1And2Naive(StringBuilder out, Running shop) throws IOException, InterruptedException {
        out.append("== 1. Importing an image from a web address ==\n");
        row(out, "honest address", shop.importFrom(HONEST));
        out.append("\n== 2. The server fetches whatever it is told ==\n");
        for (var attack : ATTACKS.entrySet()) {
            row(out, attack.getKey(), shop.importFrom(attack.getValue()));
        }
    }

    private static void act3Blocklist(StringBuilder out, Running shop) throws IOException, InterruptedException {
        out.append("\n== 3. A blocklist of names is easy to walk around ==\n");
        for (var attack : DISGUISES.entrySet()) {
            row(out, attack.getKey(), shop.importFrom(attack.getValue()));
        }
    }

    private static void act4Guarded(StringBuilder out, Running shop) throws IOException, InterruptedException {
        out.append("\n== 4. Check the address the connection really uses ==\n");
        row(out, "honest address", shop.importFrom(HONEST));
        Map<String, String> all = new LinkedHashMap<>(DISGUISES);
        all.put("localhost", "http://localhost/");
        for (var attack : all.entrySet()) {
            row(out, attack.getKey(), shop.importFrom(attack.getValue()));
        }
        row(out, "user info trick", shop.importFrom("http://images.example@169.254.169.254/"));
        row(out, "DNS rebinding (own image only)", shop.importFrom("http://rebind.attacker.example/cat.png"));
        row(out, "two answers, one private", shop.importFrom("http://dual.attacker.example/cat.png"));
        out.append("\n== 5. Limits, and why checking and then connecting is not enough ==\n");
        row(out, "port 22", shop.importFrom("http://images.example:22/"));
        row(out, "a web page, not an image", shop.importFrom("http://evil.example/page.html"));
        row(out, "an answer of 5000 bytes", shop.importFrom("http://evil.example/big.png"));
        row(out, "an answer that takes 3 seconds", shop.importFrom("http://evil.example/slow.png"));
    }

    private static void act5CheckThenConnect(StringBuilder out, Internet internet) throws IOException, InterruptedException {
        try (Running shop = start(internet, "two-step")) {
            row(out, "check, then connect by name", shop.importFrom("http://rebind.attacker.example/cat.png"));
        }
    }
}
