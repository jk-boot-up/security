package com.jk.explore.security.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import ch.qos.logback.classic.Logger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.logstash.logback.mask.MaskingJsonGeneratorDecorator;

public class LoggingDemo {

    static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl";
    static final String CARD = "4111 1111 1111 1111";
    static final String FORGED = "Mallory\nINFO login succeeded user=admin ip=10.0.0.1";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        act1(out);
        act2(out);
        act3(out);
        act4(out);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static SQLException databaseDown() {
        return new SQLException("Connection to jdbc:postgresql://db.internal.shop.example:5432/shop?user=app&password=s3cr3t-db-password refused while saving order o-1042");
    }

    static Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("name", "Ana");
        p.put("card", CARD);
        p.put("cvv", "737");
        p.put("order", "1042");
        return p;
    }

    /** The events of the lesson, written the careless way: whatever is at hand goes into the message. */
    static void careless(Logger log) {
        log.info("checkout started: {} token={}", params(), TOKEN);
        log.error("checkout failed", databaseDown());
        log.info("checkout started for {}", FORGED);
    }

    /** The fake secrets the demo looks for in each log file, by name. */
    static Map<String, String> canaries() {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("card number", CARD);
        c.put("cvv", "cvv=737");
        c.put("session token", TOKEN);
        c.put("db password", "s3cr3t-db-password");
        return c;
    }

    private static final int WIDTH = 58;

    /** Splits text into lines of at most {@code width} characters, after a comma or a space (not inside a card number), or before a '?'; nothing is lost. */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String piece : text.split("(?<=,)|(?<=\\D )|(?=\\?)")) {
            if (!line.isEmpty() && line.length() + piece.stripTrailing().length() > width) {
                lines.add(line.toString().stripTrailing());
                line.setLength(0);
            }
            line.append(piece);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString().stripTrailing());
        }
        return lines;
    }

    /** A log file's lines, numbered, each wrapped to fit, with its continuation indented. */
    private static void logFile(StringBuilder out, String header, String written) {
        List<String> lines = written.lines().toList();
        line(out, "  " + header + ", " + lines.size() + (lines.size() == 1 ? " line):" : " lines):"));
        for (int i = 0; i < lines.size(); i++) {
            List<String> parts = wrap(lines.get(i), WIDTH);
            for (int j = 0; j < parts.size(); j++) {
                line(out, (j == 0 ? "    " + (i + 1) + " " : "      ") + parts.get(j));
            }
        }
    }

    private static void canarySearch(StringBuilder out, String written, int events) {
        line(out, "  CANARY SEARCH (each fake secret, in the log file):");
        canaries().forEach((name, value) -> line(out, String.format("    %-14s %s", name, written.contains(value) ? "found" : "not found")));
        line(out, String.format("    %-14s %d events -> %d lines", "the file", events, written.lines().count()));
    }

    private static void luhn(StringBuilder out, String digits) {
        String spaced = digits.chars().mapToObj(ch -> String.valueOf((char) ch)).collect(Collectors.joining(" "));
        int[] steps = Masker.luhnSteps(digits);
        int sum = Arrays.stream(steps).sum();
        line(out, "    digits        " + spaced);
        line(out, "    every 2nd x2  " + Arrays.stream(steps).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
        line(out, "    sum           " + sum + (Masker.luhn(digits) ? ", ends in 0: a card" : ", does not end in 0: not a card"));
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. What the log keeps.");
        line(out, "  THE EVENTS (what the code logs, 3 calls):");
        line(out, "    1 info(\"checkout started: {} token={}\", params, token)");
        line(out, "    2 error(\"checkout failed\", databaseDown())");
        line(out, "    3 info(\"checkout started for {}\", name)");
        line(out, "    the name: " + FORGED.replace("\n", "\\n"));
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            careless(log.logger());
            String written = log.output();
            logFile(out, "THE LOG FILE (layout %level %msg%plainEx%n", written);
            canarySearch(out, written, 3);
        }
    }

    private static void act2(StringBuilder out) {
        line(out, "TWO. Covering the shapes of secrets in the layout.");
        try (LogSetup log = LogSetup.pattern("%level %mask%maskedEx%n")) {
            careless(log.logger());
            String written = log.output();
            logFile(out, "THE LOG FILE (layout %level %mask%maskedEx%n", written);
            canarySearch(out, written, 3);
        }
        String charge = "charge " + CARD + " for order 4111111111111112";
        String login = "login failed password=hunter2&user=ana, json {\"api_key\":\"k-123\"}, Authorization: Bearer abcdefgh12345";
        line(out, "  BEFORE MASKING:");
        for (String text : List.of(charge, login)) {
            wrap(text, WIDTH + 2).forEach(w -> line(out, "    " + w));
        }
        try (LogSetup log = LogSetup.pattern("%mask%n")) {
            log.logger().info(charge);
            log.logger().info(login);
            line(out, "  AFTER MASKING:");
            log.output().lines().forEach(l -> wrap(l, WIDTH + 2).forEach(w -> line(out, "    " + w)));
        }
        line(out, "  HOW THE LUHN CHECK IS MADE (the card, the order number):");
        luhn(out, CARD.replace(" ", ""));
        luhn(out, "4111111111111112");
    }

    private static void act3(StringBuilder out) {
        line(out, "THREE. Make the secret unprintable.");
        CheckoutRequest request = new CheckoutRequest("Ana", new Card("4111111111111111"), new Secret("737"), 1042);
        line(out, "  THE OBJECTS (what the code holds):");
        line(out, "    name   \"" + request.name() + "\"");
        line(out, "    card   new Card(\"" + request.card().number() + "\")");
        line(out, "    cvv    new Secret(\"" + request.cvv().reveal() + "\")");
        line(out, "    order  " + request.order());
        line(out, "  HOW EACH ONE PRINTS (its toString):");
        line(out, "    Card    " + request.card() + "  (the last 4 of " + request.card().number().length() + " digits)");
        line(out, "    Secret  " + request.cvv());
        line(out, "    reveal  " + request.cvv().reveal() + "  (only where the value is used)");
        try (LogSetup log = LogSetup.pattern("%level %msg%n")) {
            log.logger().info("checkout started: {}", request);
            log.logger().info("checkout started for {} with token {}", "Ana", new Secret(TOKEN));
            logFile(out, "THE LOG FILE (layout %level %msg%n, no masking", log.output());
        }
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            try {
                throw databaseDown();
            } catch (SQLException e) {
                log.logger().error("checkout failed [ref-0001], the database said: {}", e.getClass().getSimpleName());
            }
            logFile(out, "A LIBRARY'S EXCEPTION (by class, with a reference", log.output());
        }
    }

    static MaskingJsonGeneratorDecorator masking() {
        MaskingJsonGeneratorDecorator mask = new MaskingJsonGeneratorDecorator();
        mask.setDefaultMask("[REDACTED]");
        mask.addPath("/cvv");
        mask.addPath("/card/number");
        mask.addValueMasker((context, value) -> {
            if (value instanceof CharSequence text && !Masker.mask(text.toString()).equals(text.toString())) {
                return Masker.mask(text.toString());
            }
            return null;
        });
        return mask;
    }

    private static void act4(StringBuilder out) {
        line(out, "FOUR. Structured logs.");
        try (LogSetup log = LogSetup.json(null)) {
            log.mdc("requestId", "r-77");
            log.logger().info("checkout started for {}", FORGED);
            log.logger().info("charge", keyValue("order", 1042), keyValue("amount", "180.00"));
            logFile(out, "JSON EVENTS (the forged name, and fields as data", log.output());
        }
        try (LogSetup log = LogSetup.json(null)) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")), keyValue("cvv", 737));
            log.logger().info("charge", keyValue("card", new Card("4111111111111111")));
            logFile(out, "A RECORD AS JSON (PlainCard, then Card", log.output());
        }
        try (LogSetup log = LogSetup.json(masking())) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")), keyValue("cvv", 737));
            log.logger().error("checkout failed", databaseDown());
            logFile(out, "AFTER THE MASKING DECORATOR (paths and shapes", log.output());
        }
    }

    private static void act5(StringBuilder out) {
        line(out, "FIVE. What a mask cannot see.");
        String said = "customer says: my pin is 4921 and the code is hunter2, ssn 078-05-1120";
        line(out, "  BEFORE MASKING:");
        wrap(said, WIDTH + 2).forEach(w -> line(out, "    " + w));
        String masked;
        try (LogSetup log = LogSetup.pattern("%mask%n")) {
            log.logger().info(said);
            masked = log.output().stripTrailing();
        }
        line(out, "  AFTER MASKING:");
        wrap(masked, WIDTH + 2).forEach(w -> line(out, "    " + w));
        line(out, "  WHAT THE MASK KNOWS (each secret in a sentence, masked):");
        Map<String, String> samples = new LinkedHashMap<>();
        samples.put("card number", "card " + CARD);
        samples.put("cvv", "cvv=737");
        samples.put("session token", "token " + TOKEN);
        samples.put("db password", "password=s3cr3t-db-password");
        samples.put("pin", "my pin is 4921");
        samples.put("code word", "the code is hunter2");
        samples.put("ssn", "ssn 078-05-1120");
        samples.forEach((name, text) -> line(out, String.format("    %-14s %s", name, Masker.mask(text).equals(text) ? "missed" : "covered")));
        line(out, "  THE ORDER OF DEFENCE:");
        line(out, "    1 do not log it");
        line(out, "    2 make it unprintable (Secret, Card)");
        line(out, "    3 mask the shapes you know (%mask, the decorator)");
        line(out, "    4 test with fake secrets of your own (canaries)");
    }
}
