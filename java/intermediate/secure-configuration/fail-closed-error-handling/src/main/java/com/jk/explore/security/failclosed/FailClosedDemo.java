package com.jk.explore.security.failclosed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runs the bank story in five numbered acts and prints exact, repeatable results. Each act prints its input and its result in
 * separate, labelled sections, and works out derived values (totals, waiting times, counts) with the real numbers.
 */
public class FailClosedDemo {

    static final long DAILY_LIMIT = 200_000;
    static final Payment SMALL = new Payment("P-1", "4111111111111111", "corner-cafe", 450);
    static final Payment RISKY = new Payment("P-2", "4111111111111111", "unknown-electronics", 150_000);
    static final Payment BIG = new Payment("P-3", "4111111111111111", "furniture-store", 450_000);

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
        return d.approved() ? "APPROVED" : d.reason().equals("refused") ? "refused" : "refused: " + d.reason();
    }

    private static String describe(Payment p) {
        return money(p.cents()) + " at " + p.merchant();
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

    private static void act1FailingOpen(StringBuilder out) {
        line(out, "ONE. Failing open.");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.HEALTHY);
        NaiveAuthorizer naive = new NaiveAuthorizer(fraud, DAILY_LIMIT);
        line(out, "  THE RULE (NaiveAuthorizer):");
        line(out, "    start from yes; a check can only take it away");
        line(out, "  THE PAYMENT:");
        line(out, "    " + describe(RISKY));
        line(out, "  THE RESULT, FRAUD CHECK WORKING:");
        line(out, "    " + outcome(naive.authorize(RISKY, "0")) + " (looks fraudulent)");
        fraud.setMode(FraudService.Mode.BROKEN);
        line(out, "  THE RESULT, FRAUD CHECK DOWN:");
        line(out, "    " + outcome(naive.authorize(RISKY, "0")) + "  <- AN ERROR BECAME A YES");
        fraud.setMode(FraudService.Mode.HEALTHY);
        line(out, "  THE PAYMENT:");
        line(out, "    " + describe(BIG));
        line(out, "  THE DAILY LIMIT, WORKED OUT:");
        line(out, "    spent today " + money(0) + " + " + money(BIG.cents()) + " = " + money(BIG.cents()) + " > " + money(DAILY_LIMIT));
        line(out, "  THE RESULT, SPENT-TODAY READ AS \"0\":");
        line(out, "    " + outcome(naive.authorize(BIG, "0")) + " (over the daily limit)");
        line(out, "  THE RESULT, SPENT-TODAY UNREADABLE (\"n/a\"):");
        line(out, "    " + outcome(naive.authorize(BIG, "n/a")) + "  <- THE CHECK WAS SKIPPED");
        line(out, "  a check that never ran took nothing away");
    }

    private static void act2FailingClosed(StringBuilder out) {
        line(out, "TWO. Failing closed.");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.HEALTHY);
        SecurityLog log = new SecurityLog();
        SafeAuthorizer safe = new SafeAuthorizer(fraud, DAILY_LIMIT, new References(), log);
        line(out, "  THE RULE (SafeAuthorizer):");
        line(out, "    start from no; only a check that ran and passed");
        line(out, "    turns it into yes");
        record Case(String name, Payment payment, FraudService.Mode mode, String limitText, String fault) {
        }
        List<Case> cases = List.of(
                new Case("coffee", SMALL, FraudService.Mode.HEALTHY, "0", "nothing"),
                new Case("risky", RISKY, FraudService.Mode.HEALTHY, "0", "nothing"),
                new Case("risky", RISKY, FraudService.Mode.BROKEN, "0", "fraud check down"),
                new Case("coffee", SMALL, FraudService.Mode.SLOW, "0", "fraud check slow"),
                new Case("big", BIG, FraudService.Mode.HEALTHY, "n/a", "spent-today \"n/a\""));
        line(out, "  THE INPUTS (payment, what goes wrong):");
        List<String> results = new ArrayList<>();
        int number = 1;
        for (Case c : cases) {
            line(out, String.format(Locale.ROOT, "    %d  %-7s%8s  %s", number, c.name(), money(c.payment().cents()), c.fault()));
            fraud.setMode(c.mode());
            Decision d = safe.authorize(c.payment(), c.limitText());
            results.add("    " + number++ + "  " + d.reference() + "  " + outcome(d));
        }
        line(out, "  THE RESULTS:");
        results.forEach(r -> line(out, r));
        line(out, "  THE TIME BUDGET, WORKED OUT (REF-0004):");
        line(out, "    the check needs " + new FraudService(clock, FraudService.Mode.SLOW).millisNeeded() + " ms > budget "
                + SafeAuthorizer.FRAUD_BUDGET_MS + " ms");
        line(out, "    -> given up at " + SafeAuthorizer.FRAUD_BUDGET_MS + " ms: no answer is a no");
        line(out, "  THE SECURITY LOG (one line per refusal):");
        line(out, "    " + log.lines().size() + " lines: " + String.join(", ", log.lines().stream().map(l -> l.split(" ")[0]).toList()));
    }

    private static String accounts(Ledger ledger) {
        return "ana " + money(ledger.balance("ana")) + "   ben " + money(ledger.balance("ben")) + "   total " + money(ledger.total());
    }

    private static void act3PartialFailures(StringBuilder out) {
        line(out, "THREE. Failing halfway.");
        long amount = 25_000;
        Ledger naive = new Ledger(Map.of("ana", 100_000L, "ben", 0L), Set.of("ben"));
        long before = naive.total();
        line(out, "  THE ACCOUNTS BEFORE (ben's account is frozen):");
        line(out, "    " + accounts(naive));
        line(out, "  THE TRANSFER:");
        line(out, "    ana sends " + money(amount) + " to ben");
        boolean done = Transfers.naive(naive, "ana", "ben", amount);
        line(out, "  AFTER, DEBIT THEN CREDIT (Transfers.naive):");
        line(out, "    debit ana  " + money(before) + " - " + money(amount) + " = " + money(naive.balance("ana")));
        line(out, "    credit ben  fails: account ben is frozen; done " + done);
        line(out, "    " + accounts(naive));
        line(out, "    " + money(before) + " - " + money(naive.total()) + " = " + money(before - naive.total()) + "  <- VANISHED");
        Ledger atomic = new Ledger(Map.of("ana", 100_000L, "ben", 0L), Set.of("ben"));
        boolean done2 = Transfers.atomic(atomic, "ana", "ben", amount);
        line(out, "  AFTER, ONE UNIT OF WORK (Transfers.atomic):");
        line(out, "    checked first: ben accepts credit? " + atomic.acceptsCredit("ben") + " -> done " + done2);
        line(out, "    " + accounts(atomic));
        line(out, "  THE CONNECTION POOL BEFORE:");
        line(out, "    3 connections, 3 free");
        line(out, "  AFTER 3 FAILED UPDATES, GIVEN BACK ONLY ON SUCCESS:");
        ConnectionPool leaky = new ConnectionPool(3);
        line(out, "    free after each: " + afterEachFailure(leaky, false));
        String next;
        try {
            ConnectionPool.naiveUpdate(leaky, false);
            next = "works";
        } catch (IllegalStateException e) {
            next = "fails: " + e.getMessage();
        }
        line(out, "    the next update " + next);
        line(out, "  AFTER 3 FAILED UPDATES, WITH TRY-WITH-RESOURCES:");
        line(out, "    free after each: " + afterEachFailure(new ConnectionPool(3), true));
    }

    /** Runs three failing updates and lists the free connections after each one. */
    private static String afterEachFailure(ConnectionPool pool, boolean safe) {
        List<String> free = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            try {
                if (safe) {
                    ConnectionPool.safeUpdate(pool, true);
                } else {
                    ConnectionPool.naiveUpdate(pool, true);
                }
            } catch (IllegalStateException e) {
                free.add(String.valueOf(pool.free()));
            }
        }
        return String.join(", ", free);
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
        line(out, "  THE FAILURE (the fraud service's exception):");
        wrap(failure.getClass().getSimpleName() + ": " + failure.getMessage(), 48).forEach(l -> line(out, "    " + l));
        String naive = ErrorPages.naive(failure);
        line(out, "  THE RAW ERROR PAGE (the exception as it came):");
        line(out, "    names an internal server: " + naive.contains("fraud-db.internal"));
        line(out, "    shows the full card number: " + naive.contains(RISKY.card()));
        line(out, "    shows code lines (a stack trace): " + naive.contains("  at "));
        SecurityLog log = new SecurityLog();
        SafeAuthorizer safe = new SafeAuthorizer(fraud, DAILY_LIMIT, new References(), log);
        Decision d = safe.authorize(RISKY, "0");
        line(out, "  THE SAFE PAGE (what the customer sees):");
        wrap(ErrorPages.safe(d.reference()), 48).forEach(l -> line(out, "    " + l));
        line(out, "  THE LOG LINE (what support sees):");
        wrap(log.lines().get(0), 48).forEach(l -> line(out, "    " + l));
        line(out, "  THE REDACTION, WORKED OUT:");
        line(out, "    " + RISKY.card() + " -> " + SecurityLog.redact(RISKY.card()) + " (last four digits)");
        line(out, "  support finds the detail from the reference;");
        line(out, "  an attacker learns nothing from the page");
    }

    private static void act5TheBill(StringBuilder out) {
        line(out, "FIVE. The bill, and how to pay it.");
        line(out, "  THE OUTAGE:");
        line(out, "    the fraud check needs " + new FraudService(new SimClock(), FraudService.Mode.SLOW).millisNeeded()
                + " ms; the budget is " + SafeAuthorizer.FRAUD_BUDGET_MS + " ms");
        line(out, "    10 customers each pay " + money(SMALL.cents()) + " for a coffee");
        SimClock waiting = new SimClock();
        FraudService slowForAll = new FraudService(waiting, FraudService.Mode.SLOW);
        SafeAuthorizer safe = new SafeAuthorizer(slowForAll, DAILY_LIMIT, new References(), new SecurityLog());
        int approved = 0;
        for (int i = 0; i < 10; i++) {
            if (safe.authorize(SMALL, "0").approved()) {
                approved++;
            }
        }
        line(out, "  WITHOUT A BREAKER:");
        line(out, "    approved " + approved + " of 10; calls to the fraud check " + slowForAll.calls());
        line(out, "    waiting " + slowForAll.calls() + " x " + SafeAuthorizer.FRAUD_BUDGET_MS + " ms = " + waiting.now() + " ms");
        SimClock clock = new SimClock();
        FraudService fraud = new FraudService(clock, FraudService.Mode.SLOW);
        CircuitBreaker breaker = new CircuitBreaker(clock, 3, 30_000);
        int alertAfter = 5;
        ResilientAuthorizer resilient = new ResilientAuthorizer(fraud, breaker, alertAfter);
        int approved2 = 0;
        for (int i = 0; i < 10; i++) {
            if (resilient.authorize(SMALL).approved()) {
                approved2++;
            }
        }
        line(out, "  WITH A BREAKER THAT OPENS AFTER 3 FAILURES:");
        line(out, "    approved " + approved2 + " of 10; calls to the fraud check " + fraud.calls());
        line(out, "    waiting " + fraud.calls() + " x " + SafeAuthorizer.FRAUD_BUDGET_MS + " ms = " + clock.now() + " ms; breaker " + breaker.state());
        line(out, "  THE REFUSALS BY REASON:");
        int total = 0;
        for (Map.Entry<String, Integer> e : resilient.refusals().entrySet()) {
            line(out, String.format(Locale.ROOT, "    %-22s%2d", e.getKey(), e.getValue()));
            total += e.getValue();
        }
        line(out, "    total " + total + " >= alert threshold " + alertAfter + " -> alert raised " + resilient.alerted());
        clock.advance(30_000);
        fraud.setMode(FraudService.Mode.HEALTHY);
        line(out, "  AFTER THE 30000 ms COOL-DOWN, FRAUD CHECK BACK:");
        line(out, "    one trial call: a coffee " + outcome(resilient.authorize(SMALL)) + "; breaker " + breaker.state());
        line(out, "  an open breaker still says no: it fails fast, not open");
    }
}
