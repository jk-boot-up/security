package com.jk.explore.security.failclosedspring;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Runs the bank story in five numbered acts against real Spring Boot servers and a real fraud-check server on the loopback interface. Each act
 * prints its input and its result in separate, labelled sections; only statuses, booleans, balances and counts are printed, so the output is stable.
 */
public class FailClosedSpringDemo {

    static final String RISKY = "{\"card\":\"4111111111111111\",\"merchant\":\"unknown-electronics\",\"amountCents\":150000}";
    static final String COFFEE = "{\"card\":\"4111111111111111\",\"merchant\":\"corner-cafe\",\"amountCents\":450}";
    static final String CARD = "4111111111111111";

    private static final Wire WIRE = new Wire();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses, booleans, balances and counts are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Running warm = Running.start("default")) {
            WIRE.pay(warm.url(), COFFEE);   // loads the HTTP classes once, so no first call runs out of its budget while the JVM warms up
        }
        try (Running bank = Running.start("default", "bank.mode=open")) {
            act1Open(out, bank);
        }
        try (Running bank = Running.start("default", "bank.breaker.threshold=1000")) {
            act2Closed(out, bank);
        }
        try (Running bank = Running.start("default")) {
            act3Halfway(out, bank);
        }
        act4Told(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String decision(Wire.Reply r) {
        return r.status() == 200 ? "APPROVED" : "refused";
    }

    private static String prop(Running bank, String name) {
        return bank.context().getEnvironment().getProperty(name);
    }

    /** Splits a long text into lines of at most `width` characters, at spaces. */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!current.isEmpty() && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(current.isEmpty() ? "" : " ").append(word);
        }
        lines.add(current.toString());
        return lines;
    }

    private static void act1Open(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. Failing open.");
        line(out, "  THE CODE (OpenPaymentService):");
        line(out, "    start from yes; catch (Exception e) { } keeps it");
        line(out, "  THE PAYMENT (POST /payments):");
        line(out, "    1500.00 at unknown-electronics");
        FraudServer.Mode[] modes = {FraudServer.Mode.UP, FraudServer.Mode.DROP, FraudServer.Mode.ERROR, FraudServer.Mode.GARBAGE, FraudServer.Mode.SLOW};
        line(out, "  THE FRAUD CHECK (budget " + prop(bank, "bank.fraud.budget-ms") + " ms):");
        List<String> results = new ArrayList<>();
        for (int i = 0; i < modes.length; i++) {
            bank.fraud().mode(modes[i]);
            line(out, "    " + (i + 1) + "  " + describe(modes[i]));
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            results.add("    " + (i + 1) + "  " + r.status() + "  " + decision(r)
                    + (modes[i] == FraudServer.Mode.DROP ? "  <- AN ERROR BECAME A YES" : ""));
        }
        line(out, "  THE RESULTS (HTTP status, decision):");
        results.forEach(r -> line(out, r));
        line(out, "  a check that never ran took nothing away");
    }

    private static String describe(FraudServer.Mode mode) {
        return switch (mode) {
            case UP -> "works: risk high";
            case DROP -> "drops the connection";
            case ERROR -> "answers HTTP 500";
            case GARBAGE -> "answers with a maintenance page";
            case SLOW -> "takes " + FraudServer.SLOW_MS + " ms";
        };
    }

    private static void act2Closed(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "TWO. Failing closed.");
        line(out, "  THE CODE (ClosedPaymentService):");
        line(out, "    start from no; yes only when the check ran,");
        line(out, "    in time, and said low risk");
        record Case(String name, String json, FraudServer.Mode mode) {
        }
        List<Case> cases = List.of(new Case("coffee    4.50", COFFEE, FraudServer.Mode.UP), new Case("risky  1500.00", RISKY, FraudServer.Mode.UP),
                new Case("risky  1500.00", RISKY, FraudServer.Mode.ERROR), new Case("risky  1500.00", RISKY, FraudServer.Mode.GARBAGE),
                new Case("risky  1500.00", RISKY, FraudServer.Mode.DROP), new Case("coffee    4.50", COFFEE, FraudServer.Mode.SLOW));
        line(out, "  THE INPUTS (payment, fraud check):");
        List<String> results = new ArrayList<>();
        boolean quick = false;
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            line(out, "    " + (i + 1) + "  " + c.name() + "  " + (c.mode() == FraudServer.Mode.GARBAGE ? "maintenance page" : describe(c.mode()).replace(": risk high", "")));
            bank.fraud().mode(c.mode());
            long start = System.nanoTime();
            Wire.Reply r = WIRE.pay(bank.url(), c.json());
            quick = Duration.ofNanos(System.nanoTime() - start).toMillis() < FraudServer.SLOW_MS - 100;
            String verdict = r.status() == 200 ? "APPROVED" : r.status() == 403 ? "refused: looks fraudulent" : "refused, " + r.reference();
            results.add("    " + (i + 1) + "  " + r.status() + "  " + verdict);
        }
        line(out, "  THE RESULTS (HTTP status, decision):");
        results.forEach(r -> line(out, r));
        long budget = Long.parseLong(prop(bank, "bank.fraud.budget-ms"));
        long attempts = Long.parseLong(prop(bank, "bank.fraud.attempts"));
        line(out, "  THE TIME BUDGET, WORKED OUT (row 6):");
        line(out, "    " + attempts + " attempts x " + budget + " ms = " + attempts * budget + " ms < " + FraudServer.SLOW_MS + " ms");
        line(out, "    answered before the fraud check finished: " + quick);
        long refusals = bank.bean(SecurityLog.class).lines().stream().filter(l -> l.startsWith("REF-")).count();
        line(out, "  THE SECURITY LOG (one line per refusal):");
        line(out, "    " + refusals + " lines");
    }

    private static String accounts(Ledger ledger) {
        return "ana " + money(ledger.cents("ana")) + "  ben " + money(ledger.cents("ben")) + "  total " + money(ledger.total());
    }

    private static void act3Halfway(StringBuilder out, Running bank) throws InterruptedException {
        line(out, "THREE. Failing halfway.");
        Ledger ledger = bank.bean(Ledger.class);
        Transfers transfers = bank.bean(Transfers.class);
        long before = ledger.total();
        line(out, "  THE ACCOUNTS BEFORE (ben's account is frozen):");
        line(out, "    " + accounts(ledger));
        line(out, "  THE TRANSFER (four versions of the same code):");
        line(out, "    ana sends 250.00 to ben");
        record Version(String name, Attempt attempt, String note) {
        }
        line(out, "  THE RESULTS:");
        long lost = 0;
        int losing = 0;
        Version[] versions = {
                new Version("no transaction", () -> transfers.withoutTransaction("ana", "ben", 25000), "  <- DEBIT STAYED"),
                new Version("@Transactional, checked exception", () -> transfers.transactionalButChecked("ana", "ben", 25000), "  <- NO ROLLBACK"),
                new Version("@Transactional, called from the same class", () -> transfers.callsItsOwnTransactionalMethod("ana", "ben", 25000), "  <- NO PROXY"),
                new Version("@Transactional(rollbackFor = Exception.class)", () -> transfers.unitOfWork("ana", "ben", 25000), "")};
        for (Version v : versions) {
            ledger.reset();
            try {
                v.attempt().run();
            } catch (FrozenAccountException e) {
                // the credit failed; what matters is what is left in the accounts
            }
            line(out, "    " + v.name());
            line(out, "      " + accounts(ledger) + v.note());
            if (ledger.total() < before) {
                lost = before - ledger.total();
                losing++;
            }
        }
        line(out, "  THE LOSS, WORKED OUT:");
        line(out, "    " + money(before) + " - " + money(before - lost) + " = " + money(lost) + " vanished, in " + losing + " of " + versions.length);
        line(out, "  only the last one is a unit of work");
    }

    @FunctionalInterface
    private interface Attempt {
        void run() throws FrozenAccountException;
    }

    private static String money(long cents) {
        return "%d.%02d".formatted(cents / 100, cents % 100);
    }

    private static void act4Told(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. What the customer is told, and what the log keeps.");
        line(out, "  THE FAILURE:");
        line(out, "    the fraud check drops the connection");
        try (Running bank = Running.start("leaky")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            line(out, "  THE RESPONSE, DEBUGGING SETTINGS ON (leaky):");
            leaks(out, r, "  <- A MAP OF THE BANK");
        }
        try (Running bank = Running.start("default")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            line(out, "  THE RESPONSE, WITH ONE ERROR HANDLER:");
            leaks(out, r, "");
            String told = "The payment could not be completed. Please try again later, quoting reference " + r.reference() + ".";
            line(out, "  THE CUSTOMER SEES (in the response: " + r.body().contains(told) + "):");
            wrap(told, 48).forEach(l -> line(out, "    " + l));
            line(out, "  THE LOG LINE (what support sees):");
            wrap(bank.bean(SecurityLog.class).lines().get(0), 48).forEach(l -> line(out, "    " + l));
            line(out, "  support finds the detail from the reference;");
            line(out, "  an attacker learns nothing from the page");
        }
    }

    private static void leaks(StringBuilder out, Wire.Reply r, String mark) {
        line(out, "    HTTP " + r.status() + "; internal address 127.0.0.1: " + r.body().contains("127.0.0.1"));
        line(out, "    full card number: " + r.body().contains(CARD));
        line(out, "    code lines (.java): " + r.body().contains(".java") + mark);
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill, and how to pay it.");
        try (Running bank = Running.start("default", "bank.fraud.budget-ms=150", "bank.breaker.threshold=1000")) {
            line(out, "  THE OUTAGE:");
            line(out, "    the fraud check needs " + FraudServer.SLOW_MS + " ms; the budget is " + prop(bank, "bank.fraud.budget-ms") + " ms");
            line(out, "    10 customers each pay 4.50 for a coffee");
            bank.fraud().mode(FraudServer.Mode.SLOW);
            int approved = tenCoffees(bank);
            line(out, "  WITHOUT A BREAKER (" + prop(bank, "bank.fraud.attempts") + " attempts per payment):");
            line(out, "    approved " + approved + " of 10");
            line(out, "    calls: 10 payments x " + prop(bank, "bank.fraud.attempts") + " = " + bank.fraud().calls());
        }
        try (Running bank = Running.start("default", "bank.fraud.budget-ms=150")) {
            String threshold = prop(bank, "bank.breaker.threshold");
            bank.fraud().mode(FraudServer.Mode.SLOW);
            int approved = tenCoffees(bank);
            line(out, "  WITH A BREAKER THAT OPENS AFTER " + threshold + " FAILED PAYMENTS:");
            line(out, "    approved " + approved + " of 10; breaker " + bank.bean(Breaker.class).state());
            line(out, "    calls: " + threshold + " payments x " + prop(bank, "bank.fraud.attempts") + " = " + bank.fraud().calls()
                    + "; the other " + (10 - Integer.parseInt(threshold)) + " refused at once");
            RefusalWatcher watcher = bank.bean(RefusalWatcher.class);
            line(out, "  THE REFUSALS BY REASON (Micrometer counters):");
            int total = 0;
            for (Map.Entry<String, Integer> e : watcher.byReason().entrySet()) {
                line(out, "    %-22s%2d".formatted(e.getKey(), e.getValue()));
                total += e.getValue();
            }
            line(out, "    total " + total + " >= alert threshold " + prop(bank, "bank.alert.refusals") + " -> alert raised " + watcher.alert());
            bank.fraud().mode(FraudServer.Mode.UP);
            bank.bean(MutableClock.class).advance(Duration.ofSeconds(31));
            line(out, "  AFTER THE " + prop(bank, "bank.breaker.cool-down-seconds") + " s COOL-DOWN, FRAUD CHECK BACK:");
            Wire.Reply back = WIRE.pay(bank.url(), COFFEE);
            line(out, "    one trial call: a coffee " + decision(back) + "; breaker " + bank.bean(Breaker.class).state());
        }
        line(out, "  an open breaker still says no: it fails fast, not open");
    }

    private static int tenCoffees(Running bank) {
        return (int) IntStream.range(0, 10).filter(i -> {
            try {
                return WIRE.pay(bank.url(), COFFEE).status() == 200;
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }).count();
    }
}
