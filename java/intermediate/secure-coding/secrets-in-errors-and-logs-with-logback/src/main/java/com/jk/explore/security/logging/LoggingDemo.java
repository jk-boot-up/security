package com.jk.explore.security.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import ch.qos.logback.classic.Logger;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private static String indent(String text) {
        return text.lines().map(l -> "    " + l).reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. What the log keeps.");
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            careless(log.logger());
            String written = log.output();
            line(out, "  three events written with a plain layout, as lines:");
            line(out, indent(written.stripTrailing()));
            line(out, "  the card, the cvv, the token and the database password are all in the file; so is the forged line: " + written.lines().count() + " lines from 3 events  <- THE LOG IS A SECOND DATABASE THAT NOBODY PROTECTS");
        }
    }

    private static void act2(StringBuilder out) {
        line(out, "TWO. Covering the shapes of secrets in the layout.");
        try (LogSetup log = LogSetup.pattern("%level %mask%maskedEx%n")) {
            careless(log.logger());
            line(out, "  the same three events through %mask and %maskedEx:");
            line(out, indent(log.output().stripTrailing()));
        }
        try (LogSetup log = LogSetup.pattern("%level %mask%n")) {
            log.logger().info("charge {} for order {}", CARD, "4111111111111112");
            log.logger().info("login failed password=hunter2&user=ana, json {\"api_key\":\"k-123\"}, Authorization: Bearer abcdefgh12345");
            line(out, indent(log.output().stripTrailing()));
        }
        line(out, "  a 16-digit order number that fails the Luhn check is left alone, and a card that passes it is covered: the layout reads the text, so it knows only the shapes it was given");
        line(out, "  the line break in the forged name became the two characters \\n: one event is one line");
    }

    private static void act3(StringBuilder out) {
        line(out, "THREE. Make the secret unprintable.");
        CheckoutRequest request = new CheckoutRequest("Ana", new Card("4111111111111111"), new Secret("737"), 1042);
        try (LogSetup log = LogSetup.pattern("%level %msg%n")) {
            log.logger().info("checkout started: {}", request);
            log.logger().info("checkout started for {} with token {}", "Ana", new Secret(TOKEN));
            line(out, "  a plain layout, no masking, and types that print safely:");
            line(out, indent(log.output().stripTrailing()));
        }
        line(out, "  the value is read where it is used, by name: " + request.cvv().reveal().length() + " digits, and a search for reveal( finds every place that does");
        try (LogSetup log = LogSetup.pattern("%level %msg%plainEx%n")) {
            try {
                throw databaseDown();
            } catch (SQLException e) {
                log.logger().error("checkout failed [ref-0001], the database said: {}", e.getClass().getSimpleName());
            }
            line(out, indent(log.output().stripTrailing()));
        }
        line(out, "  an exception from a library carries whatever the library put in its message, so it is logged by class, with a reference, or through %maskedEx");
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
            line(out, "  JSON events: fields are data, and a line break in a value is escaped, so the forged name stays one event of one line");
            line(out, indent(log.output().stripTrailing()));
        }
        try (LogSetup log = LogSetup.json(null)) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")), keyValue("cvv", 737));
            log.logger().info("charge", keyValue("card", new Card("4111111111111111")));
            line(out, "  a record is written as its fields, not as its toString; a Card with @JsonValue is written as ****1111:");
            line(out, indent(log.output().stripTrailing()));
        }
        try (LogSetup log = LogSetup.json(masking())) {
            log.logger().info("charge", keyValue("card", new PlainCard("4111111111111111", "Ana")), keyValue("cvv", 737));
            log.logger().error("checkout failed", databaseDown());
            line(out, "  the same through the masking decorator: paths for /cvv and /card/number, and a masker for the shapes of secrets in any string:");
            line(out, indent(log.output().stripTrailing()));
        }
    }

    private static void act5(StringBuilder out) {
        line(out, "FIVE. The bill.");
        try (LogSetup log = LogSetup.pattern("%level %mask%n")) {
            log.logger().info("customer says: my pin is 4921 and the code is hunter2, ssn 078-05-1120");
            line(out, "  a secret in a shape nobody listed: " + log.output().stripTrailing().substring(5) + "  <- A MASK CANNOT COVER WHAT IT HAS NEVER SEEN");
        }
        line(out, "  so the order of defence is: do not log it, make it unprintable, then cover the shapes you know, and test the last one with the secrets you really have");
        line(out, "  a log file is data: who can read it, how long it is kept, who can delete a person from it, and where it is copied are decisions, not defaults");
        line(out, "  debug logging in production is a second logging policy; set the level, and check what a higher level adds");
        line(out, "  masking costs a pass over every message: do it where the line is written, not where it is read");
    }
}
