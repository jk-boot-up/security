package com.jk.explore.security.leaks;

import java.util.Map;
import java.util.TreeMap;

/** Runs the checkout story in four numbered acts and prints exact, repeatable results. All card numbers are standard test numbers. */
public class LeaksDemo {

    static final String CARD = "4111 1111 1111 1111"; // a standard test card number: valid check digit, not a real account
    static final Map<String, String> ORDER = new TreeMap<>(Map.of("name", "Ana", "order", "1042", "card", CARD, "cvv", "737"));
    static final Secret TOKEN = new Secret("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmEifQ.c2lnbmF0dXJl");
    static final String FORGED_NAME = "Mallory\nINFO login succeeded user=admin ip=10.0.0.1";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("== 1. What the customer sees when the database is down ==\n");
        LogSink naiveSink = new LogSink();
        Checkout naive = Checkout.naive(new NaiveLogger(naiveSink));
        Checkout.Response r = naive.pay(ORDER, TOKEN);
        out.append(r.status()).append(' ').append(r.body()).append('\n');

        out.append("\n== 2. What the log keeps ==\n");
        naiveSink.lines().forEach(l -> out.append(l).append('\n'));
        LogSink forgedSink = new LogSink();
        Checkout.naive(new NaiveLogger(forgedSink)).pay(new TreeMap<>(Map.of("name", FORGED_NAME, "order", "7")), TOKEN);
        out.append("a customer whose name holds a line break -> ").append(forgedSink.lines().get(0).lines().count()).append(" log lines from one event; the second reads:\n");
        out.append("  ").append(forgedSink.lines().get(0).lines().skip(1).findFirst().orElse("")).append('\n');

        out.append("\n== 3. A generic answer, and the details where only the team looks ==\n");
        LogSink safeSink = new LogSink();
        Checkout fixed = Checkout.fixed(new SafeLogger(safeSink));
        Checkout.Response f = fixed.pay(ORDER, TOKEN);
        out.append(f.status()).append(' ').append(f.body()).append('\n');
        safeSink.lines().forEach(l -> out.append(l).append('\n'));

        out.append("\n== 4. Redaction, wrappers and forged lines ==\n");
        Redactor red = new Redactor();
        out.append("card number in text -> ").append(red.redact("charge 4111-1111-1111-1111 for order 1042")).append('\n');
        out.append("an order number that is not a card -> ").append(red.redact("order 4111111111111112 shipped")).append('\n');
        out.append("password pair -> ").append(red.redact("login failed password=hunter2&user=ana")).append('\n');
        out.append("JSON pair -> ").append(red.redact("{\"user\":\"ana\",\"api_key\":\"AKIA123456\"}")).append('\n');
        out.append("bearer token -> ").append(red.redact("Authorization: Bearer abc123.def456.ghi789")).append('\n');
        out.append("password in a URL -> ").append(red.redact(Database.URL)).append('\n');
        out.append("a Secret in a message -> ").append("token " + TOKEN).append('\n');
        LogSink fixedForged = new LogSink();
        Checkout.fixed(new SafeLogger(fixedForged)).pay(new TreeMap<>(Map.of("name", FORGED_NAME, "order", "7")), TOKEN);
        out.append("the forged name, fixed logger -> ").append(fixedForged.lines().get(0).lines().count()).append(" line: ").append(fixedForged.lines().get(0)).append('\n');
        return out.toString();
    }
}
