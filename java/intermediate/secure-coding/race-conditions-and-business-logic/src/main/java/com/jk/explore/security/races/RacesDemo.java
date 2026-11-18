package com.jk.explore.security.races;

import java.util.List;
import java.util.function.BiFunction;

/** Runs the bank and store story in five numbered acts and prints exact, repeatable results. */
public class RacesDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    /**
     * The whole demo as text, so the tests can compare it with the golden file. Concurrency is shown with the step-by-step model and
     * a written order of steps, never with real thread timing, so the output is the same on every run.
     */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1CheckThenAct(out);
        act2Atomic(out);
        act3Idempotency(out);
        act4BusinessRules(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A "key   value" line inside a section. */
    private static void fact(StringBuilder out, String key, Object value) {
        line(out, "    " + String.format("%-13s", key) + value);
    }

    private static void trace(StringBuilder out, Interleaving.Result result) {
        for (String step : result.trace()) {
            line(out, "    " + step);
        }
    }

    /** Runs two withdrawals of 80 from 100 in every order and counts the orders that pay out more than the account held. */
    private static int overdrawing(List<String> orders, BiFunction<Ledger, String, Steps> withdrawal) {
        int bad = 0;
        for (String order : orders) {
            Ledger ledger = new Ledger().open("ANA-1", 100);
            Interleaving.run(order, withdrawal.apply(ledger, "A"), withdrawal.apply(ledger, "B"));
            if (ledger.paidOut("ANA-1") > 100) {
                bad++;
            }
        }
        return bad;
    }

    private static void act1CheckThenAct(StringBuilder out) {
        line(out, "ONE. Check, then act: two requests in the gap.");
        line(out, "  THE REQUESTS (ana's account ANA-1 holds 100):");
        fact(out, "A", "withdraw 80, from her phone");
        fact(out, "B", "withdraw 80, from her laptop, at the same moment");
        fact(out, "each", "reads the balance, checks it covers 80, writes");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        NaiveWithdrawal a = new NaiveWithdrawal("A", ledger, "ANA-1", 80);
        NaiveWithdrawal b = new NaiveWithdrawal("B", ledger, "ANA-1", 80);
        line(out, "  THE STEPS, IN THE ORDER ABABAB:");
        trace(out, Interleaving.run("ABABAB", a, b));
        line(out, "  THE RESULT:");
        fact(out, "A", a.outcome());
        fact(out, "B", b.outcome());
        fact(out, "paid out", "80 + 80 = " + ledger.paidOut("ANA-1") + " from a balance of 100  <- OVERDRAWN");
        fact(out, "balance", ledger.balance("ANA-1") + ": B wrote 100 - 80 over A's 20");

        Ledger alone = new Ledger().open("ANA-1", 100);
        NaiveWithdrawal c = new NaiveWithdrawal("A", alone, "ANA-1", 80);
        NaiveWithdrawal d = new NaiveWithdrawal("B", alone, "ANA-1", 80);
        Interleaving.Result inTurn = Interleaving.run("AAABBB", c, d);
        line(out, "  ONE AFTER THE OTHER, IN THE ORDER AAABBB:");
        fact(out, "A", c.outcome());
        fact(out, "B", d.outcome() + ": " + inTurn.trace().get(4));

        List<String> orders = EveryOrder.of(3, 3);
        line(out, "  EVERY ORDER OF A'S 3 STEPS AND B'S 3 STEPS (" + orders.size() + "):");
        StringBuilder row = new StringBuilder("   ");
        int bad = 0;
        for (int i = 0; i < orders.size(); i++) {
            Ledger l = new Ledger().open("ANA-1", 100);
            Interleaving.run(orders.get(i), new NaiveWithdrawal("A", l, "ANA-1", 80), new NaiveWithdrawal("B", l, "ANA-1", 80));
            bad += l.paidOut("ANA-1") > 100 ? 1 : 0;
            row.append(String.format(" %s %-4d", orders.get(i), l.paidOut("ANA-1")));
            if (i % 5 == 4) {
                line(out, row.toString().stripTrailing());
                row = new StringBuilder("   ");
            }
        }
        fact(out, "pays 160", bad + " of " + orders.size() + " orders");
        fact(out, "pays 80", (orders.size() - bad) + ": only one request after the other");

        CouponBook book = new CouponBook();
        CouponRedemption first = new CouponRedemption("A", book, "SUMMER20", 20, false);
        CouponRedemption second = new CouponRedemption("B", book, "SUMMER20", 20, false);
        line(out, "  A SINGLE-USE COUPON SUMMER20 (20 off), IN THE ORDER ABABAB:");
        trace(out, Interleaving.run("ABABAB", first, second));
        line(out, "  THE RESULT:");
        fact(out, "discount", "20 + 20 = " + book.discountGiven() + " instead of 20  <- THE SAME GAP");
    }

    private static void act2Atomic(StringBuilder out) {
        line(out, "TWO. Making the check and the write one step.");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        LockedWithdrawal a = new LockedWithdrawal("A", ledger, "ANA-1", "ANA-1", 80);
        LockedWithdrawal b = new LockedWithdrawal("B", ledger, "ANA-1", "ANA-1", 80);
        line(out, "  WITH A LOCK ON ANA-1, IN THE ORDER ABABABAB:");
        trace(out, Interleaving.run("ABABABAB", a, b));
        line(out, "  THE RESULT:");
        fact(out, "A", a.outcome());
        fact(out, "B", b.outcome());
        fact(out, "paid out", ledger.paidOut("ANA-1") + ", balance 100 - 80 = " + ledger.balance("ANA-1"));

        Ledger casLedger = new Ledger().open("ANA-1", 100);
        CasWithdrawal c = new CasWithdrawal("A", casLedger, "ANA-1", 80);
        CasWithdrawal d = new CasWithdrawal("B", casLedger, "ANA-1", 80);
        line(out, "  WITH A COMPARE-AND-SET, NO LOCK, IN THE ORDER ABABAB:");
        trace(out, Interleaving.run("ABABAB", c, d));
        line(out, "  HOW B'S COMPARE-AND-SET DECIDES:");
        fact(out, "B read", "100");
        fact(out, "now", "20, written by A after B read");
        fact(out, "100 = 20?", "no: nothing is written, B reads again");
        line(out, "  THE RESULT:");
        fact(out, "A", c.outcome());
        fact(out, "B", d.outcome());
        fact(out, "paid out", casLedger.paidOut("ANA-1") + ", balance " + casLedger.balance("ANA-1"));

        CouponBook book = new CouponBook();
        CouponRedemption first = new CouponRedemption("A", book, "SUMMER20", 20, true);
        CouponRedemption second = new CouponRedemption("B", book, "SUMMER20", 20, true);
        line(out, "  THE COUPON, CLAIMED IN ONE STEP, IN THE ORDER AB:");
        trace(out, Interleaving.run("AB", first, second));
        fact(out, "discount", book.discountGiven());

        List<String> six = EveryOrder.of(3, 3);
        List<String> eight = EveryOrder.of(4, 4);
        line(out, "  EVERY ORDER, AND HOW MANY PAY OUT MORE THAN 100:");
        line(out, String.format("    %-17s%d of %d", "check, then act",
                overdrawing(six, (l, n) -> new NaiveWithdrawal(n, l, "ANA-1", 80)), six.size()));
        line(out, String.format("    %-17s%d of %d", "lock",
                overdrawing(eight, (l, n) -> new LockedWithdrawal(n, l, "ANA-1", "ANA-1", 80)), eight.size()));
        line(out, String.format("    %-17s%d of %d", "compare-and-set",
                overdrawing(six, (l, n) -> new CasWithdrawal(n, l, "ANA-1", 80)), six.size()));
    }

    private static void act3Idempotency(StringBuilder out) {
        line(out, "THREE. Sending the same request twice.");
        Bank.Transfer rent = new Bank.Transfer("ANA-1", "BEN-2", 300);
        line(out, "  THE REQUEST (ana pays ben 300 rent):");
        fact(out, "transfer", "ANA-1 -> BEN-2, 300");
        fact(out, "balances", "ana 1000, ben 0");
        fact(out, "the reply", "is lost, so the app sends the same request again");
        Bank plain = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        line(out, "  BEFORE THE FIX (no key):");
        fact(out, "send 1", plain.transfer(rent) + ": ana " + plain.balance("ANA-1") + ", ben " + plain.balance("BEN-2"));
        fact(out, "send 2", plain.transfer(rent) + ": ana " + plain.balance("ANA-1") + ", ben " + plain.balance("BEN-2")
                + "  <- PAID TWICE");
        Bank keyed = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        line(out, "  AFTER THE FIX (the app sends the key rent-october):");
        fact(out, "send 1", keyed.transfer("rent-october", rent) + ": ana " + keyed.balance("ANA-1") + ", ben " + keyed.balance("BEN-2"));
        fact(out, "send 2", keyed.transfer("rent-october", rent) + ": ana " + keyed.balance("ANA-1"));
        fact(out, "amount 900", keyed.transfer("rent-october", new Bank.Transfer("ANA-1", "BEN-2", 900)));
        fact(out, "new key", "rent-november: " + keyed.transfer("rent-november", rent) + ": ana " + keyed.balance("ANA-1"));
        line(out, "  HOW THE BANK READS A KEY:");
        fact(out, "new key", "claim it in one step, move, keep the answer");
        fact(out, "repeated", "return the kept answer, move nothing");
        fact(out, "changed", "refuse: a key names one request");
        fact(out, "two at once", "only one claim succeeds, the other waits");
    }

    private static void act4BusinessRules(StringBuilder out) {
        line(out, "FOUR. Rules the server must keep, whatever the client sends.");
        line(out, "  THE REQUESTS (ana 1000, ben 1000; 1 reward point a transfer):");
        fact(out, "amount", "ANA-1 -> BEN-2, -50");
        fact(out, "to herself", "ANA-1 -> ANA-1, 1, sent ten times");
        fact(out, "too much", "ANA-1 -> BEN-2, 5000");
        Bank careless = new Bank(false).open("ANA-1", 1000).open("BEN-2", 1000);
        careless.transfer(new Bank.Transfer("ANA-1", "BEN-2", -50));
        line(out, "  BEFORE THE FIX (a careless bank):");
        fact(out, "-50", "ana " + careless.balance("ANA-1") + ", ben " + careless.balance("BEN-2") + "  <- MONEY TAKEN FROM BEN");
        int pointsBefore = careless.rewardPoints("ANA-1");
        for (int i = 0; i < 10; i++) {
            careless.transfer(new Bank.Transfer("ANA-1", "ANA-1", 1));
        }
        fact(out, "to herself", "ana " + careless.balance("ANA-1") + ", "
                + (careless.rewardPoints("ANA-1") - pointsBefore) + " points  <- POINTS FOR NOTHING");
        Bank careful = new Bank(true).open("ANA-1", 1000).open("BEN-2", 1000);
        line(out, "  AFTER THE FIX (the careful bank):");
        fact(out, "-50", careful.transfer(new Bank.Transfer("ANA-1", "BEN-2", -50)));
        fact(out, "to herself", careful.transfer(new Bank.Transfer("ANA-1", "ANA-1", 1)));
        fact(out, "5000", careful.transfer(new Bank.Transfer("ANA-1", "BEN-2", 5000)));

        Checkout.Request cart = new Checkout.Request(List.of(new Checkout.Line("boots-42", 1)),
                List.of("SUMMER20", "WELCOME20", "SPRING20"), 1);
        line(out, "  THE CART, AS THE CLIENT SENT IT:");
        fact(out, "lines", "boots-42 x 1 (the catalogue says " + Checkout.CATALOGUE.get("boots-42") + ")");
        fact(out, "coupons", String.join(" ", cart.coupons()) + ", 20 off each");
        fact(out, "total", cart.totalFromClient() + ", written by the client");
        Checkout.Priced believed = Checkout.careless(cart);
        line(out, "  BEFORE THE FIX (the client's total, every coupon):");
        fact(out, "charged", believed.working() + " = " + believed.charged() + "  <- THE STORE PAYS");
        Checkout.Priced priced = Checkout.careful(cart);
        line(out, "  AFTER THE FIX (catalogue prices, one coupon):");
        fact(out, "charged", priced.working() + " = " + priced.charged());
        for (String note : priced.notes()) {
            line(out, "      " + note);
        }

        Order.Careless flags = new Order.Careless(180);
        line(out, "  AN ORDER OF 180 KEPT AS FLAGS (BEFORE THE FIX):");
        flags.ship();
        fact(out, "ship", "shipped " + flags.shipped() + ", paid " + flags.paid());
        flags.refund();
        fact(out, "refund", "refunded " + flags.refunded());
        flags.refund();
        fact(out, "refund", "refunded " + flags.refunded() + "  <- REFUNDED TWICE, NEVER PAID");
        Order order = new Order(180);
        line(out, "  THE SAME ORDER AS A STATE MACHINE (AFTER THE FIX):");
        fact(out, "ship", order.ship());
        fact(out, "pay", order.pay());
        fact(out, "ship", order.ship());
        fact(out, "refund", order.refund());
        fact(out, "refund", order.refund());
        fact(out, "refunded", order.refunded() + " in all");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        Interleaving.Result waits = Interleaving.run("ABABABAB",
                new LockedWithdrawal("A", ledger, "ANA-1", "ANA-1", 10),
                new LockedWithdrawal("B", ledger, "ANA-1", "ANA-1", 10));
        line(out, "  WAITING (the lock from act TWO, order ABABABAB):");
        fact(out, "B", "waited " + waits.waits() + " steps while A held the lock");

        Ledger accounts = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
        LockingTransfer a = new LockingTransfer("A", accounts, "ANA-1", "BEN-2", 50, false);
        LockingTransfer b = new LockingTransfer("B", accounts, "BEN-2", "ANA-1", 70, false);
        line(out, "  TWO TRANSFERS, EACH LOCKING ITS OWN SENDER FIRST:");
        fact(out, "A", "ANA-1 -> BEN-2, 50: locks " + a.lockOrder());
        fact(out, "B", "BEN-2 -> ANA-1, 70: locks " + b.lockOrder());
        line(out, "  THE STEPS, IN THE ORDER AB:");
        trace(out, Interleaving.run("AB", a, b));

        Ledger ordered = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
        LockingTransfer c = new LockingTransfer("A", ordered, "ANA-1", "BEN-2", 50, true);
        LockingTransfer d = new LockingTransfer("B", ordered, "BEN-2", "ANA-1", 70, true);
        line(out, "  ONE LOCK ORDER FOR EVERYONE (the smaller account first):");
        fact(out, "A", "ANA-1 -> BEN-2, 50: locks " + c.lockOrder());
        fact(out, "B", "BEN-2 -> ANA-1, 70: locks " + d.lockOrder());
        line(out, "  THE STEPS, IN THE ORDER AB:");
        Interleaving.Result fine = Interleaving.run("AB", c, d);
        trace(out, fine);
        line(out, "  THE RESULT:");
        fact(out, "deadlock", fine.deadlock() ? "yes" : "no");
        fact(out, "ana", "500 - 50 + 70 = " + ordered.balance("ANA-1"));
        fact(out, "ben", "500 + 50 - 70 = " + ordered.balance("BEN-2"));

        Ledger shared = new Ledger().open("ANA-1", 100);
        LockedWithdrawal one = new LockedWithdrawal("A", shared, "ANA-1", "server-1:ANA-1", 80);
        LockedWithdrawal two = new LockedWithdrawal("B", shared, "ANA-1", "server-2:ANA-1", 80);
        Interleaving.Result servers = Interleaving.run("ABABABAB", one, two);
        line(out, "  TWO SERVERS, ONE DATABASE, EACH LOCK IN ITS OWN MEMORY:");
        line(out, "    " + servers.trace().get(0));
        line(out, "    " + servers.trace().get(1));
        fact(out, "A", one.outcome());
        fact(out, "B", two.outcome());
        fact(out, "paid out", shared.paidOut("ANA-1") + " from 100  <- THE LOCKS NEVER MEET");
        line(out, "  AFTER THE FIX (the -with-postgres version):");
        fact(out, "database", "row locks, isolation levels or a version check");
    }
}
