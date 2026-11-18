package com.jk.explore.security.leaks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Runs the checkout story in four numbered acts and prints exact, repeatable results. All card numbers are standard test numbers. */
public class LeaksDemo {

    static final String CARD = "4111 1111 1111 1111"; // a standard test card number: valid check digit, not a real account
    static final Map<String, String> ORDER = new TreeMap<>(Map.of("name", "Ana", "order", "1042", "card", CARD, "cvv", "737"));
    static final Secret TOKEN = new Secret("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl");
    static final String FORGED_NAME = "Mallory\nINFO login succeeded user=admin ip=10.0.0.1";
    static final Map<String, String> FORGED_ORDER = new TreeMap<>(Map.of("name", FORGED_NAME, "order", "7"));
    static final Canaries CANARIES = new Canaries(canaryValues());

    /** The texts that act four puts through the redactor, labelled a to g. */
    static final List<String> SAMPLES = List.of(
            "charge 4111-1111-1111-1111 for order 1042",
            "order 4111111111111112 shipped",
            "login failed password=hunter2&user=ana",
            "{\"user\":\"ana\",\"api_key\":\"AKIA123456\"}",
            "Authorization: Bearer abc123.def456.ghi789",
            "postgresql://app:s3cr3t-db-password@db.internal:5432/shop",
            "the key is hunter2");

    private static final int WIDTH = 60;

    public static void main(String[] args) {
        System.out.print(run());
    }

    private static Map<String, String> canaryValues() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("card number", CARD);
        m.put("cvv", "737");
        m.put("session token", TOKEN.reveal());
        m.put("db password", "s3cr3t-db-password");
        return m;
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("ONE. What the customer sees when the database is down.\n");
        LogSink naiveSink = new LogSink();
        Checkout.Response page = Checkout.naive(new NaiveLogger(naiveSink)).pay(ORDER, TOKEN);
        out.append("  THE REQUEST (POST /checkout):\n");
        for (String k : List.of("name", "order", "card", "cvv")) {
            out.append(String.format("    %-6s %s%n", k, ORDER.get(k)));
        }
        out.append(String.format("    %-6s %s%n", "token", TOKEN.reveal()));
        out.append("  THE RESPONSE (status ").append(page.status()).append(", naive checkout):\n");
        page.body().lines().forEach(l -> wrap(l.strip(), WIDTH + 2).forEach(w -> out.append("    ").append(w).append('\n')));
        out.append("  WHAT A VISITOR LEARNS (read from the response):\n");
        PageReader.read(page.body()).forEach((k, v) -> out.append(String.format("    %-9s  %s%n", k, v)));

        out.append("TWO. What the log keeps.\n");
        logFile(out, "naive logger", naiveSink.lines());
        canarySearch(out, page, naiveSink);
        out.append("  THE REQUEST (a name with a line break, shown as \\n):\n");
        out.append(String.format("    %-6s %s%n", "name", FORGED_NAME.replace("\n", "\\n")));
        out.append(String.format("    %-6s %s%n", "order", FORGED_ORDER.get("order")));
        LogSink forgedSink = new LogSink();
        Checkout.naive(new NaiveLogger(forgedSink)).pay(FORGED_ORDER, TOKEN);
        logFile(out, "naive logger, 1 event", forgedSink.lines().subList(0, 1));

        out.append("THREE. A generic answer, and the details where only the team looks.\n");
        LogSink safeSink = new LogSink();
        Checkout.Response fixed = Checkout.fixed(new SafeLogger(safeSink)).pay(ORDER, TOKEN);
        out.append("  THE RESPONSE (status ").append(fixed.status()).append(", fixed checkout):\n");
        out.append("    ").append(fixed.body()).append('\n');
        logFile(out, "fixed logger", safeSink.lines());
        String ref = fixed.body().replaceAll(".*(ref-\\d+).*", "$1");
        List<String> physical = physicalLines(safeSink.lines());
        out.append("  HOW SUPPORT FINDS THE DETAIL:\n");
        out.append("    the customer quotes   ").append(ref).append('\n');
        out.append("    the log line with it  ").append(physical.indexOf(physical.stream().filter(l -> l.contains("[" + ref + "]")).findFirst().orElseThrow()) + 1).append('\n');
        canarySearch(out, fixed, safeSink);
        LogSink fixedForged = new LogSink();
        Checkout.fixed(new SafeLogger(fixedForged)).pay(FORGED_ORDER, TOKEN);
        logFile(out, "fixed logger, the same event", fixedForged.lines().subList(0, 1));

        out.append("FOUR. Redaction: what it finds and what it misses.\n");
        Redactor red = new Redactor();
        out.append("  BEFORE REDACTION:\n");
        for (int i = 0; i < SAMPLES.size(); i++) {
            out.append("    ").append((char) ('a' + i)).append("  ").append(SAMPLES.get(i)).append('\n');
        }
        out.append("  AFTER REDACTION:\n");
        for (int i = 0; i < SAMPLES.size(); i++) {
            out.append("    ").append((char) ('a' + i)).append("  ").append(red.redact(SAMPLES.get(i))).append('\n');
        }
        out.append("  THE RULE THAT MASKED IT:\n");
        for (int i = 0; i < SAMPLES.size(); i++) {
            List<String> rules = red.rulesThatMatch(SAMPLES.get(i));
            out.append("    ").append((char) ('a' + i)).append("  ").append(rules.isEmpty() ? "none: left as it was" : String.join(", ", rules)).append('\n');
        }
        out.append("  HOW THE LUHN CHECK IS MADE (a and b):\n");
        for (String digits : List.of(SAMPLES.get(0).replaceAll("\\D", "").substring(0, 16), SAMPLES.get(1).replaceAll("\\D", ""))) {
            int[] c = Luhn.contributions(digits);
            int sum = Luhn.sum(digits);
            out.append("    digits        ").append(digits.chars().mapToObj(ch -> String.valueOf((char) ch)).collect(Collectors.joining(" "))).append('\n');
            out.append("    every 2nd x2  ").append(Arrays.stream(c).mapToObj(String::valueOf).collect(Collectors.joining(" "))).append('\n');
            out.append("    sum           ").append(sum).append(Luhn.valid(digits) ? ", ends in 0: a card" : ", does not end in 0: not a card").append('\n');
        }
        out.append("  A SECRET IN A MESSAGE (\"token \" + TOKEN):\n");
        out.append("    printed   ").append("token " + TOKEN).append('\n');
        out.append("    reveal()  ").append(TOKEN.reveal()).append('\n');
        return out.toString();
    }

    /** Every line as it lands in the file: an entry holding a line break becomes two lines. */
    private static List<String> physicalLines(List<String> entries) {
        return entries.stream().flatMap(String::lines).toList();
    }

    private static void logFile(StringBuilder out, String who, List<String> entries) {
        List<String> lines = physicalLines(entries);
        out.append("  THE LOG FILE (").append(who).append(", ").append(lines.size()).append(lines.size() == 1 ? " line):\n" : " lines):\n");
        for (int i = 0; i < lines.size(); i++) {
            List<String> parts = wrap(lines.get(i), WIDTH);
            for (int j = 0; j < parts.size(); j++) {
                out.append(j == 0 ? "    " + (i + 1) + " " : "      ").append(parts.get(j)).append('\n');
            }
        }
    }

    private static void canarySearch(StringBuilder out, Checkout.Response page, LogSink sink) {
        String log = String.join("\n", sink.lines());
        out.append("  CANARY SEARCH (each fake secret, in the page and the log):\n");
        out.append(String.format("    %-14s %-5s %s%n", "secret", "page", "log"));
        for (String name : CANARIES.names()) {
            out.append(String.format("    %-14s %-5s %s%n", name, CANARIES.foundIn(name, page.body()) ? "yes" : "no", CANARIES.foundIn(name, log) ? "yes" : "no"));
        }
    }

    /** Splits text into lines of at most {@code width} characters at spaces; a word longer than that (a URL) is split before its '?'. */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String[] parts = word.length() > width ? word.split("(?=\\?)") : new String[] {word};
            for (int i = 0; i < parts.length; i++) {
                String sep = line.isEmpty() || i > 0 ? "" : " ";
                if (!line.isEmpty() && line.length() + sep.length() + parts[i].length() > width) {
                    lines.add(line.toString());
                    line.setLength(0);
                    sep = "";
                }
                line.append(sep).append(parts[i]);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
