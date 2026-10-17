package com.jk.explore.security.failclosed;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Runs the bank story in five numbered acts and prints exact, repeatable results. */
public class FailClosedDemo {

    static final long DAILY_LIMIT = 200_000;
    static final Payment SMALL = new Payment("P-1", "4111111111111111", "corner-cafe", 450);
    static final Payment RISKY = new Payment("P-2", "4111111111111111", "unknown-electronics", 150_000);

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1FailingOpen(out);
        act2FailingClosed(out);
        act3PartialFailures(out);
        act4ErrorMessages(out);
        act5TheBill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String money(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private static String outcome(Decision d) {
        return d.approved() ? "APPROVED" : d.reason().equals("refused") ? "refused" : "refused (" + d.reason() + ")";
    }

    private static void act1FailingOpen(StringBuilder out) {
        line(out, "ONE. Failing open.");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.HEALTHY);
        NaiveAuthorizer naive = new NaiveAuthorizer(fraud, DAILY_LIMIT);
        line(out, "  a payment of " + money(RISKY.cents()) + " at " + RISKY.merchant() + ", fraud check working: " + outcome(naive.authorize(RISKY, "0")));
        fraud.setMode(FraudService.Mode.BROKEN);
        line(out, "  the same payment while the fraud check is down: " + outcome(naive.authorize(RISKY, "0"))
                + "  <- AN ERROR BECAME A YES");
        fraud.setMode(FraudService.Mode.HEALTHY);
        Payment big = new Payment("P-3", "4111111111111111", "furniture-store", 450_000);
        line(out, "  a payment of " + money(big.cents()) + " against a daily limit of " + money(DAILY_LIMIT) + ": " + outcome(naive.authorize(big, "0")));
        line(out, "  the same payment when the account's spent-today value is unreadable: " + outcome(naive.authorize(big, "n/a"))
                + "  <- AN EXCEPTION SKIPPED THE CHECK");
        line(out, "  the decision started as yes, and each check could only take it away: a check that never ran took nothing away");
    }

    private static void act2FailingClosed(StringBuilder out) {
        line(out, "TWO. Failing closed.");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.HEALTHY);
        SecurityLog log = new SecurityLog();
        SafeAuthorizer safe = new SafeAuthorizer(fraud, clock, DAILY_LIMIT, new References(), log);
        line(out, "  the decision starts as no, and only a check that ran and passed can turn it into yes");
        line(out, "  a coffee for " + money(SMALL.cents()) + ": " + outcome(safe.authorize(SMALL, "0")));
        line(out, "  the risky payment, fraud check working: " + outcome(safe.authorize(RISKY, "0")));
        fraud.setMode(FraudService.Mode.BROKEN);
        Decision down = safe.authorize(RISKY, "0");
        line(out, "  the risky payment while the fraud check is down: " + outcome(down) + ", reference " + down.reference());
        fraud.setMode(FraudService.Mode.SLOW);
        line(out, "  a coffee while the fraud check takes 5 seconds, with a budget of " + SafeAuthorizer.FRAUD_BUDGET_MS + " ms: " + outcome(safe.authorize(SMALL, "0")));
        fraud.setMode(FraudService.Mode.HEALTHY);
        Payment big = new Payment("P-3", "4111111111111111", "furniture-store", 450_000);
        line(out, "  the big payment with an unreadable spent-today value: " + outcome(safe.authorize(big, "n/a")));
        line(out, "  every refusal is in the log with its reference: " + log.lines().size() + " lines");
    }

    private static void act3PartialFailures(StringBuilder out) {
        line(out, "THREE. Failing halfway.");
        Ledger naive = new Ledger(Map.of("ana", 100_000L, "ben", 0L), Set.of("ben"));
        long before = naive.total();
        boolean done = Transfers.naive(naive, "ana", "ben", 25_000);
        line(out, "  ana sends " + money(25_000) + " to ben, whose account is frozen: done " + done + ", ana now has " + money(naive.balance("ana"))
                + ", ben has " + money(naive.balance("ben")));
        line(out, "  money in the bank before " + money(before) + ", after " + money(naive.total()) + "  <- " + money(before - naive.total()) + " VANISHED");
        Ledger atomic = new Ledger(Map.of("ana", 100_000L, "ben", 0L), Set.of("ben"));
        boolean done2 = Transfers.atomic(atomic, "ana", "ben", 25_000);
        line(out, "  as one unit of work, checked first and undone if the second half fails: done " + done2 + ", ana still has "
                + money(atomic.balance("ana")) + ", money in the bank " + money(atomic.total()));
        ConnectionPool leaky = new ConnectionPool(3);
        for (int i = 0; i < 3; i++) {
            try {
                ConnectionPool.naiveUpdate(leaky, true);
            } catch (IllegalStateException e) {
                // the update failed
            }
        }
        String next;
        try {
            ConnectionPool.naiveUpdate(leaky, false);
            next = "works";
        } catch (IllegalStateException e) {
            next = "fails: " + e.getMessage();
        }
        line(out, "  three failed updates that give a connection back only on success: free connections " + leaky.free() + ", the next update " + next);
        ConnectionPool safe = new ConnectionPool(3);
        for (int i = 0; i < 3; i++) {
            try {
                ConnectionPool.safeUpdate(safe, true);
            } catch (IllegalStateException e) {
                // the update failed
            }
        }
        line(out, "  the same three failures with try-with-resources: free connections " + safe.free());
    }

    private static void act4ErrorMessages(StringBuilder out) {
        line(out, "FOUR. What the customer is told, and what the log keeps.");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.BROKEN);
        Exception failure;
        try {
            fraud.risky(RISKY);
            failure = null;
        } catch (ServiceException e) {
            failure = e;
        }
        String naive = ErrorPages.naive(failure);
        line(out, "  the raw error page names an internal server: " + naive.contains("fraud-db.internal")
                + ", shows the full card number: " + naive.contains(RISKY.card()) + ", shows code lines: " + naive.contains("  at "));
        SecurityLog log = new SecurityLog();
        SafeAuthorizer safe = new SafeAuthorizer(fraud, clock, DAILY_LIMIT, new References(), log);
        Decision d = safe.authorize(RISKY, "0");
        String page = ErrorPages.safe(d.reference());
        line(out, "  the customer sees: \"" + page + "\"");
        String logged = log.lines().get(0);
        line(out, "  the log line has the reference: " + logged.contains(d.reference()) + ", the reason: " + logged.contains("connection refused")
                + ", the full card number: " + logged.contains(RISKY.card()) + ", the last four digits: " + logged.contains("****1111"));
        line(out, "  support finds the detail from the reference, and an attacker learns nothing from the page");
    }

    private static void act5TheBill(StringBuilder out) {
        line(out, "FIVE. The bill, and how to pay it.");
        SimClock waiting = new SimClock();
        FraudService slowForAll = new FraudService(waiting, FraudService.Mode.SLOW);
        SafeAuthorizer safe = new SafeAuthorizer(slowForAll, waiting, DAILY_LIMIT, new References(), new SecurityLog());
        int approved = 0;
        for (int i = 0; i < 10; i++) {
            if (safe.authorize(SMALL, "0").approved()) {
                approved++;
            }
        }
        line(out, "  failing closed costs availability: 10 coffees while the fraud check is slow, approved " + approved
                + ", time spent waiting " + waiting.now() / 1000 + " seconds");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.SLOW);
        CircuitBreaker breaker = new CircuitBreaker(clock, 3, 30_000);
        ResilientAuthorizer resilient = new ResilientAuthorizer(fraud, clock, breaker, 5);
        int approved2 = 0;
        for (int i = 0; i < 10; i++) {
            if (resilient.authorize(SMALL).approved()) {
                approved2++;
            }
        }
        line(out, "  with a circuit breaker that opens after 3 failures: approved " + approved2 + ", calls to the fraud check " + fraud.calls()
                + ", time spent waiting " + clock.now() / 1000 + " seconds, breaker " + breaker.state());
        line(out, "  refusals by reason: " + resilient.refusals() + ", alert raised: " + resilient.alerted());
        clock.advance(30_000);
        fraud.setMode(FraudService.Mode.HEALTHY);
        line(out, "  after the cool-down, with the fraud check back: a coffee " + outcome(resilient.authorize(SMALL)) + ", breaker " + breaker.state());
        line(out, "  an open breaker still says no: it fails fast, not open, and the refusals are watched so a long outage is noticed");
        List<String> rules = List.of("start from no", "a budget for every call", "a limited retry", "a breaker that refuses", "an alert on refusals");
        line(out, "  the rules: " + String.join(", ", rules));
    }
}
