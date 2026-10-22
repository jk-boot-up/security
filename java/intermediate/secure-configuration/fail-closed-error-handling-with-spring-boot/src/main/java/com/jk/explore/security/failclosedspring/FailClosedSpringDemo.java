package com.jk.explore.security.failclosedspring;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.IntStream;

/** Runs the bank story in five numbered acts against real Spring Boot servers and a real fraud-check server on the loopback interface. */
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

    private static void act1Open(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "ONE. Failing open.");
        line(out, "  a payment of 1500.00 at unknown-electronics, fraud check working: " + decision(WIRE.pay(bank.url(), RISKY)));
        for (FraudServer.Mode mode : new FraudServer.Mode[] {FraudServer.Mode.DROP, FraudServer.Mode.ERROR, FraudServer.Mode.GARBAGE, FraudServer.Mode.SLOW}) {
            bank.fraud().mode(mode);
            line(out, "  the same payment while the fraud check " + describe(mode) + ": " + decision(WIRE.pay(bank.url(), RISKY))
                    + (mode == FraudServer.Mode.DROP ? "  <- AN ERROR BECAME A YES" : ""));
        }
        line(out, "  the decision started as yes, and a catch-all kept it: a check that never ran took nothing away");
    }

    private static String describe(FraudServer.Mode mode) {
        return switch (mode) {
            case DROP -> "drops the connection";
            case ERROR -> "answers 500";
            case GARBAGE -> "answers with a maintenance page";
            case SLOW -> "takes 2 seconds, with a budget of 500 ms";
            case UP -> "works";
        };
    }

    private static void act2Closed(StringBuilder out, Running bank) throws IOException, InterruptedException {
        line(out, "TWO. Failing closed.");
        line(out, "  the decision starts as no, and only a fraud check that ran, in time, and said low risk turns it into yes");
        line(out, "  a coffee for 4.50: " + decision(WIRE.pay(bank.url(), COFFEE)));
        Wire.Reply risky = WIRE.pay(bank.url(), RISKY);
        line(out, "  the risky payment, fraud check working: " + decision(risky) + " (" + risky.status() + ", looks fraudulent)");
        for (FraudServer.Mode mode : new FraudServer.Mode[] {FraudServer.Mode.ERROR, FraudServer.Mode.GARBAGE, FraudServer.Mode.DROP}) {
            bank.fraud().mode(mode);
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            line(out, "  the risky payment while the fraud check " + describe(mode) + ": " + decision(r) + " (" + r.status() + ", could not be checked), reference " + r.reference());
        }
        bank.fraud().mode(FraudServer.Mode.SLOW);
        long start = System.nanoTime();
        Wire.Reply slow = WIRE.pay(bank.url(), COFFEE);
        boolean quick = Duration.ofNanos(System.nanoTime() - start).toMillis() < 1900;
        line(out, "  a coffee while the fraud check takes 2 seconds, with a budget of 500 ms: " + decision(slow) + " (" + slow.status() + ", could not be checked), and the answer came before the fraud check finished: " + quick);
        long refusals = bank.bean(SecurityLog.class).lines().stream().filter(l -> l.startsWith("REF-")).count();
        line(out, "  every refusal is in the log with its reference: " + refusals + " lines");
    }

    private static void act3Halfway(StringBuilder out, Running bank) throws InterruptedException {
        line(out, "THREE. Failing halfway.");
        Ledger ledger = bank.bean(Ledger.class);
        Transfers transfers = bank.bean(Transfers.class);
        long before = ledger.total();
        line(out, "  ana sends 250.00 to ben, whose account is frozen, in four versions of the same code; the money in the bank before is " + money(before));
        record Version(String name, Attempt attempt, String note) {
        }
        for (Version v : new Version[] {
                new Version("no transaction", () -> transfers.withoutTransaction("ana", "ben", 25000), "  <- THE DEBIT STAYED"),
                new Version("@Transactional, checked exception", () -> transfers.transactionalButChecked("ana", "ben", 25000), "  <- SPRING DOES NOT ROLL BACK A CHECKED EXCEPTION"),
                new Version("@Transactional called from the same class", () -> transfers.callsItsOwnTransactionalMethod("ana", "ben", 25000), "  <- THE PROXY WAS SKIPPED"),
                new Version("@Transactional(rollbackFor = Exception.class), called from outside", () -> transfers.unitOfWork("ana", "ben", 25000), "")}) {
            ledger.reset();
            boolean done = true;
            try {
                v.attempt().run();
            } catch (FrozenAccountException e) {
                done = false;
            }
            line(out, "  " + v.name() + ": done " + done + ", ana now " + money(ledger.cents("ana")) + ", ben " + money(ledger.cents("ben"))
                    + ", money in the bank " + money(ledger.total()) + v.note());
        }
        line(out, "  only the last one is a unit of work: the failed credit undoes the debit too");
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
        try (Running bank = Running.start("leaky")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            line(out, "  Spring Boot's error response with the debugging settings on: " + r.status() + ", names an internal server: " + r.body().contains("127.0.0.1")
                    + ", shows the full card number: " + r.body().contains(CARD) + ", shows code lines: " + r.body().contains(".java") + "  <- THE ERROR RESPONSE IS A MAP OF THE BANK");
        }
        try (Running bank = Running.start("default")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = WIRE.pay(bank.url(), RISKY);
            line(out, "  the same failure with one error handler: " + r.status() + ", names an internal server: " + r.body().contains("127.0.0.1")
                    + ", shows the full card number: " + r.body().contains(CARD) + ", shows code lines: " + r.body().contains(".java"));
            line(out, "  the customer sees: \"The payment could not be completed. Please try again later, quoting reference " + r.reference() + ".\""
                    + " in the response: " + r.body().contains("The payment could not be completed. Please try again later, quoting reference " + r.reference() + "."));
            String logged = bank.bean(SecurityLog.class).lines().get(0);
            line(out, "  the log line has the reference: " + logged.contains(r.reference()) + ", the reason: " + logged.contains("fraud check unavailable")
                    + ", the full card number: " + logged.contains(CARD) + ", the last four digits: " + logged.contains("1111"));
            line(out, "  support finds the detail from the reference, and an attacker learns nothing from the page");
        }
    }

    private static void act5Bill(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill, and how to pay it.");
        try (Running bank = Running.start("default", "bank.fraud.budget-ms=150", "bank.breaker.threshold=1000")) {
            bank.fraud().mode(FraudServer.Mode.SLOW);
            int approved = tenCoffees(bank);
            line(out, "  failing closed costs availability: 10 coffees while the fraud check is slow, approved " + approved
                    + ", calls that reached the fraud check " + bank.fraud().calls() + " (one retry each)");
        }
        try (Running bank = Running.start("default", "bank.fraud.budget-ms=150")) {
            bank.fraud().mode(FraudServer.Mode.SLOW);
            int approved = tenCoffees(bank);
            line(out, "  with a breaker that opens after 3 failed payments: approved " + approved + ", calls that reached the fraud check " + bank.fraud().calls()
                    + ", breaker " + bank.bean(Breaker.class).state());
            RefusalWatcher watcher = bank.bean(RefusalWatcher.class);
            line(out, "  refusals by reason: " + watcher.byReason() + ", alert raised: " + watcher.alert());
            bank.fraud().mode(FraudServer.Mode.UP);
            bank.bean(MutableClock.class).advance(Duration.ofSeconds(31));
            Wire.Reply back = WIRE.pay(bank.url(), COFFEE);
            line(out, "  after the cool-down, with the fraud check back: a coffee " + decision(back) + ", breaker " + bank.bean(Breaker.class).state());
        }
        line(out, "  an open breaker still says no: it fails fast, not open, and the refusals are watched so a long outage is noticed");
        line(out, "  the rules: start from no, a budget for every call, a limited retry, a breaker that refuses, one error handler, an alert on refusals");
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
