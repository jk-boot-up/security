package com.jk.explore.security.ssrfspring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Runs the product image import story in six numbered acts against real Spring Boot servers and a small invented internet on loopback.
 * Each act prints the requests, the importer's own log of how it reached its answer, and the responses, in separate labelled sections.
 */
public class SsrfSpringDemo {

    static {
        // read once by the JDK's HTTP client; the build also sets it on the command line
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    static final String HONEST = "http://images.example/cat.png";
    static final String METADATA = "http://169.254.169.254/latest/meta-data/";
    static final String LOCALHOST = "http://localhost/";
    static final String OFFICE = "http://10.0.0.5/";
    static final String DECIMAL = "http://2852039166/latest/meta-data/";
    static final String SHORT_LOOPBACK = "http://127.1/";
    static final String MAPPED = "http://[::ffff:a9fe:a9fe]/latest/meta-data/";
    static final String INSIDE_NAME = "http://inside.evil.example/";
    static final String REDIRECT = "http://evil.example/redirect";
    static final String USER_INFO = "http://images.example@169.254.169.254/";
    static final String DUAL = "http://dual.evil.example/cat.png";
    static final String REBINDING = "http://rebind.evil.example/cat.png";

    /** The invented internet: a name (or an address with no name) and what answers there. */
    private static final String[][] HOSTS = {
            {"images.example", "a public image host"}, {"evil.example", "the attacker's host"},
            {"169.254.169.254", "cloud metadata service"}, {"localhost", "the shop's settings"}, {"10.0.0.5", "office admin page"}};

    /** What the shop answered: the HTTP status and the body. */
    record Answer(int status, String body) {
        @Override
        public String toString() {
            return status + " " + body;
        }
    }

    /** A shop started on a free loopback port with one importer. Closing it stops the server. */
    record Running(ConfigurableApplicationContext context, int port, Internet internet) implements AutoCloseable {

        Answer importFrom(String address) throws IOException, InterruptedException {
            internet.resetNameServer();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/import"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(address))).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return new Answer(response.statusCode(), response.body());
        }

        /** The importer's log of the last import. */
        List<String> trace() {
            return context.getBean(Importer.class).trace();
        }

        @Override
        public void close() {
            context.close();
        }
    }

    /** The body the staff form posts: the address, percent-encoded. */
    static String form(String address) {
        return "url=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
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
    public static String run() throws IOException, InterruptedException, ImportRefused {
        StringBuilder out = new StringBuilder();
        try (Internet internet = new Internet()) {
            try (Running shop = start(internet, "naive")) {
                act1(out, shop, internet);
                act2(out, shop);
            }
            try (Running shop = start(internet, "blocklist")) {
                act3(out, shop, internet);
            }
            try (Running shop = start(internet, "guarded")) {
                act4(out, shop);
                act5(out, shop);
                try (Running twoStep = start(internet, "two-step")) {
                    act6(out, twoStep, shop);
                }
            }
        }
        return out.toString();
    }

    private static void act1(StringBuilder out, Running shop, Internet internet) throws IOException, InterruptedException {
        line(out, "ONE. The shop's import form works.");
        line(out, "  THE INVENTED INTERNET (address, name, what answers):");
        for (String[] h : HOSTS) {
            String address = internet.resolve(h[0]).get(0).getHostAddress();
            line(out, String.format("    %-16s %-15s %s", address, address.equals(h[0]) ? "(no name)" : h[0], h[1]));
        }
        line(out, "  THE REQUEST (the staff form, posted to the shop):");
        line(out, "    POST /import");
        line(out, "    " + form(HONEST));
        line(out, "  HOW THE FORM VALUE IS DECODED:");
        TreeMap<String, String> escapes = new TreeMap<>();
        Matcher m = Pattern.compile("%[0-9A-F]{2}").matcher(form(HONEST));
        while (m.find()) {
            escapes.put(m.group(), URLDecoder.decode(m.group(), StandardCharsets.UTF_8));
        }
        escapes.forEach((code, ch) -> line(out, "    " + code + " is hex " + code.substring(1) + " = " + Integer.parseInt(code.substring(1), 16)
                + ", the character '" + ch + "'"));
        line(out, "    -> " + URLDecoder.decode(form(HONEST).substring(4), StandardCharsets.UTF_8));
        Answer answer = shop.importFrom(HONEST);
        line(out, "  HOW THE SHOP FETCHES IT (the importer's log):");
        trace(out, shop);
        line(out, "  THE RESPONSE (status, then body):");
        line(out, "    " + answer);
    }

    private static void act2(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "TWO. The server fetches whatever it is told.");
        List<String> attacks = List.of(METADATA, LOCALHOST, OFFICE);
        requests(out, "THE REQUESTS (the url the form posts):", attacks);
        responses(out, shop, attacks);
        shop.importFrom(METADATA);
        line(out, "  HOW a WAS FETCHED (the importer's log):");
        trace(out, shop);
    }

    private static void act3(StringBuilder out, Running shop, Internet internet) throws IOException, InterruptedException, ImportRefused {
        line(out, "THREE. A blocklist of names is easy to walk around.");
        line(out, "  THE BLOCKLIST (compared with the host, as text):");
        line(out, "    " + String.join(", ", BlocklistImporter.BLOCKED));
        List<String> disguises = List.of(METADATA, DECIMAL, SHORT_LOOPBACK, MAPPED, INSIDE_NAME, REDIRECT);
        requests(out, "THE REQUESTS:", disguises);
        responses(out, shop, disguises);
        line(out, "  WHAT JAVA CONNECTS TO (the host as typed -> address):");
        for (int i = 1; i < disguises.size(); i++) {
            String host = Target.parse(disguises.get(i)).host();
            line(out, String.format("    %c  %-19s -> %s", 'a' + i, host, internet.resolve(host).get(0).getHostAddress()));
        }
        shop.importFrom(REDIRECT);
        line(out, "    then " + shop.trace().get(2));
        InetAddress decimal = internet.resolve("2852039166").get(0);
        line(out, "  HOW 2852039166 IS 169.254.169.254 (base 256):");
        byte[] b = decimal.getAddress();
        long sum = 0;
        for (int i = 0; i < 4; i++) {
            long weight = 1L << (8 * (3 - i));
            long part = (b[i] & 0xff) * weight;
            sum += part;
            line(out, String.format("    %3d x %-8d = %10d", b[i] & 0xff, weight, part));
        }
        line(out, String.format("    %-14s = %10d", "sum", sum));
    }

    private static void act4(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FOUR. Check the address the connection really uses.");
        line(out, "  THE REQUEST (f, the redirect, again):");
        line(out, "    " + REDIRECT);
        Answer answer = shop.importFrom(REDIRECT);
        line(out, "  THE CHECKS, AS THE IMPORTER LOGS THEM:");
        trace(out, shop);
        line(out, "  THE RESPONSE (status, then body):");
        line(out, "    " + answer);
        List<String> all = List.of(HONEST, METADATA, DECIMAL, SHORT_LOOPBACK, MAPPED, INSIDE_NAME, LOCALHOST, USER_INFO, DUAL);
        requests(out, "THE REQUESTS:", all);
        responses(out, shop, all);
        shop.importFrom(DUAL);
        line(out, "  ONE NAME, TWO ANSWERS (request i):");
        line(out, "    " + shop.trace().get(1));
        line(out, "    " + shop.trace().get(2));
    }

    private static void act5(StringBuilder out, Running shop) throws IOException, InterruptedException {
        line(out, "FIVE. Limits on what is fetched.");
        line(out, "  THE LIMITS (from the code):");
        line(out, "    port    " + GuardedImporter.PORTS.get(0) + " or " + GuardedImporter.PORTS.get(1));
        line(out, "    type    image/...");
        line(out, "    size    at most " + Fetcher.MAX_BYTES + " bytes");
        line(out, "    time    an answer within " + Fetcher.TIMEOUT.toMillis() + " ms");
        List<String> limits = List.of("http://images.example:22/", "http://evil.example/page.html", "http://evil.example/big.png",
                "http://evil.example/slow.png");
        requests(out, "THE REQUESTS:", limits);
        responses(out, shop, limits);
    }

    private static void act6(StringBuilder out, Running twoStep, Running guarded) throws IOException, InterruptedException {
        line(out, "SIX. A second lookup can give a different answer.");
        line(out, "  THE REQUEST:");
        line(out, "    " + REBINDING);
        Answer fooled = twoStep.importFrom(REBINDING);
        line(out, "  CHECK, THEN CONNECT BY NAME (the two-step importer's log):");
        trace(out, twoStep);
        line(out, "  THE RESPONSE:");
        line(out, "    " + fooled + "  <- NOT CHECKED");
        Answer safe = guarded.importFrom(REBINDING);
        line(out, "  THE GUARDED IMPORTER (looks the name up once):");
        trace(out, guarded);
        line(out, "  THE RESPONSE:");
        line(out, "    " + safe + "  <- NOTHING INSIDE");
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void requests(StringBuilder out, String header, List<String> addresses) {
        line(out, "  " + header);
        for (int i = 0; i < addresses.size(); i++) {
            line(out, "    " + (char) ('a' + i) + "  " + addresses.get(i));
        }
    }

    private static void responses(StringBuilder out, Running shop, List<String> addresses) throws IOException, InterruptedException {
        line(out, "  THE RESPONSES (status, then body):");
        for (int i = 0; i < addresses.size(); i++) {
            line(out, "    " + (char) ('a' + i) + "  " + shop.importFrom(addresses.get(i)));
        }
    }

    private static void trace(StringBuilder out, Running shop) {
        for (String t : shop.trace()) {
            line(out, "    " + t);
        }
    }
}
