package com.jk.explore.security.races;

import java.util.List;

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

    private static void trace(StringBuilder out, Interleaving.Result result) {
        for (String step : result.trace()) {
            line(out, "    " + step);
        }
    }

    private static void act1CheckThenAct(StringBuilder out) {
        line(out, "ONE. Check, then act: two requests in the gap.");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        NaiveWithdrawal a = new NaiveWithdrawal("A", ledger, "ANA-1", 80);
        NaiveWithdrawal b = new NaiveWithdrawal("B", ledger, "ANA-1", 80);
        line(out, "  ana has 100 and sends two withdrawals of 80 at the same moment, from her phone and her laptop");
        trace(out, Interleaving.run("ABABAB", a, b));
        line(out, "  A: " + a.outcome() + ", B: " + b.outcome() + "; the bank paid out " + ledger.paidOut("ANA-1")
                + " from a balance of 100, and the balance says " + ledger.balance("ANA-1") + "  <- BOTH CHECKS PASSED BEFORE EITHER WROTE");
        Ledger alone = new Ledger().open("ANA-1", 100);
        NaiveWithdrawal c = new NaiveWithdrawal("A", alone, "ANA-1", 80);
        NaiveWithdrawal d = new NaiveWithdrawal("B", alone, "ANA-1", 80);
        Interleaving.run("AAABBB", c, d);
        line(out, "  the same two requests one after the other: A " + c.outcome() + ", B " + d.outcome()
                + ", so a test that sends them in turn never sees the bug");
        CouponBook book = new CouponBook();
        CouponRedemption first = new CouponRedemption("A", book, "SAVE20", 20, false);
        CouponRedemption second = new CouponRedemption("B", book, "SAVE20", 20, false);
        Interleaving.run("ABABAB", first, second);
        line(out, "  a single-use coupon checked, applied, then marked used, sent twice at once: discount given "
                + book.discountGiven() + " instead of 20  <- THE SAME GAP");
    }

    private static void act2Atomic(StringBuilder out) {
        line(out, "TWO. Making the check and the write one step.");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        LockedWithdrawal a = new LockedWithdrawal("A", ledger, "ANA-1", "ANA-1", 80);
        LockedWithdrawal b = new LockedWithdrawal("B", ledger, "ANA-1", "ANA-1", 80);
        line(out, "  with a lock on the account around read, check and write, in the same order of steps:");
        Interleaving.Result locked = Interleaving.run("ABABABAB", a, b);
        trace(out, locked);
        line(out, "  A: " + a.outcome() + ", B: " + b.outcome() + "; paid out " + ledger.paidOut("ANA-1") + ", balance " + ledger.balance("ANA-1"));
        Ledger casLedger = new Ledger().open("ANA-1", 100);
        CasWithdrawal c = new CasWithdrawal("A", casLedger, "ANA-1", 80);
        CasWithdrawal d = new CasWithdrawal("B", casLedger, "ANA-1", 80);
        line(out, "  without a lock, with a compare-and-set that writes only if the balance is still what was read:");
        trace(out, Interleaving.run("ABABAB", c, d));
        line(out, "  A: " + c.outcome() + ", B: " + d.outcome() + "; paid out " + casLedger.paidOut("ANA-1") + ", balance " + casLedger.balance("ANA-1"));
        CouponBook book = new CouponBook();
        CouponRedemption first = new CouponRedemption("A", book, "SAVE20", 20, true);
        CouponRedemption second = new CouponRedemption("B", book, "SAVE20", 20, true);
        Interleaving.run("AB", first, second);
        line(out, "  the coupon claimed in one step before the discount: A " + first.outcome() + ", B " + second.outcome()
                + ", discount given " + book.discountGiven());
        line(out, "  the rule that must always hold, the balance never below zero and each coupon used once, now holds in every order of steps");
    }

    private static void act3Idempotency(StringBuilder out) {
        line(out, "THREE. Sending the same request twice.");
        Bank.Transfer rent = new Bank.Transfer("ANA-1", "BEN-2", 300);
        Bank plain = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        plain.transfer(rent);
        plain.transfer(rent);
        line(out, "  ana pays 300 rent, the reply is lost, and her app sends the request again: without a key, ana has "
                + plain.balance("ANA-1") + " and ben " + plain.balance("BEN-2") + "  <- PAID TWICE");
        Bank keyed = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
        line(out, "  with an idempotency key chosen by the app for this payment: first send: " + keyed.transfer("rent-october", rent));
        line(out, "  the same key again: " + keyed.transfer("rent-october", rent) + "; ana has " + keyed.balance("ANA-1")
                + " and ben " + keyed.balance("BEN-2"));
        line(out, "  the same key with a different amount: " + keyed.transfer("rent-october", new Bank.Transfer("ANA-1", "BEN-2", 900)));
        line(out, "  a new key is a new payment: " + keyed.transfer("rent-november", rent) + "; ana has " + keyed.balance("ANA-1"));
        line(out, "  the key is claimed in one step before the money moves, so two copies racing each other cannot both run");
    }

    private static void act4BusinessRules(StringBuilder out) {
        line(out, "FOUR. Rules the server must keep, whatever the client sends.");
        Bank careless = new Bank(false).open("ANA-1", 1000).open("BEN-2", 1000);
        careless.transfer(new Bank.Transfer("ANA-1", "BEN-2", -50));
        line(out, "  a careless bank given a transfer of -50 from ana to ben: ana has " + careless.balance("ANA-1")
                + " and ben " + careless.balance("BEN-2") + "  <- MONEY TAKEN FROM BEN");
        int pointsBefore = careless.rewardPoints("ANA-1");
        for (int i = 0; i < 10; i++) {
            careless.transfer(new Bank.Transfer("ANA-1", "ANA-1", 1));
        }
        line(out, "  ten transfers of 1 from ana to herself: ana still has " + careless.balance("ANA-1") + " and earns "
                + (careless.rewardPoints("ANA-1") - pointsBefore) + " reward points  <- POINTS FOR NOTHING");
        Bank careful = new Bank(true).open("ANA-1", 1000).open("BEN-2", 1000);
        line(out, "  the careful bank: -50: " + careful.transfer(new Bank.Transfer("ANA-1", "BEN-2", -50))
                + "; to herself: " + careful.transfer(new Bank.Transfer("ANA-1", "ANA-1", 1))
                + "; 5000: " + careful.transfer(new Bank.Transfer("ANA-1", "BEN-2", 5000)));
        Checkout.Request cart = new Checkout.Request(List.of(new Checkout.Line("boots-42", 1)),
                List.of("SAVE20", "WELCOME20", "SPRING20"), 1);
        line(out, "  a cart with boots at 180, three coupons and a total of 1 written by the client:");
        line(out, "    careless checkout charges " + Checkout.careless(cart).charged() + "  <- THE STORE OWES THE CUSTOMER");
        Checkout.Priced priced = Checkout.careful(cart);
        line(out, "    careful checkout charges " + priced.charged());
        for (String note : priced.notes()) {
            line(out, "      " + note);
        }
        Order.Careless flags = new Order.Careless(180);
        flags.ship();
        flags.refund();
        flags.refund();
        line(out, "  an order kept as flags: shipped before payment " + (flags.shipped() && !flags.paid())
                + ", refunded " + flags.refunded() + " on an order of 180  <- REFUNDED TWICE, NEVER PAID");
        Order order = new Order(180);
        line(out, "  the same order as a state machine: ship first: " + order.ship() + "; pay: " + order.pay()
                + "; ship: " + order.ship() + "; refund: " + order.refund() + "; refund again: " + order.refund());
        line(out, "    refunded " + order.refunded() + " in all");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        Ledger ledger = new Ledger().open("ANA-1", 100);
        LockedWithdrawal a = new LockedWithdrawal("A", ledger, "ANA-1", "ANA-1", 10);
        LockedWithdrawal b = new LockedWithdrawal("B", ledger, "ANA-1", "ANA-1", 10);
        Interleaving.Result waits = Interleaving.run("ABABABAB", a, b);
        line(out, "  a lock makes requests to one account take turns: in the order above, B spent " + waits.waits()
                + " steps waiting while A held the lock");
        Ledger accounts = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
        Interleaving.Result deadlock = Interleaving.run("AB",
                new LockingTransfer("A", accounts, "ANA-1", "BEN-2", 50, false),
                new LockingTransfer("B", accounts, "BEN-2", "ANA-1", 70, false));
        line(out, "  two transfers in opposite directions, each locking its own sender first:");
        trace(out, deadlock);
        Ledger ordered = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
        LockingTransfer c = new LockingTransfer("A", ordered, "ANA-1", "BEN-2", 50, true);
        LockingTransfer d = new LockingTransfer("B", ordered, "BEN-2", "ANA-1", 70, true);
        Interleaving.Result fine = Interleaving.run("AB", c, d);
        line(out, "  the same two with every lock taken in one order, the smaller account first: deadlock " + fine.deadlock()
                + "; A " + c.outcome() + ", B " + d.outcome() + "; ana has " + ordered.balance("ANA-1") + ", ben " + ordered.balance("BEN-2"));
        Ledger shared = new Ledger().open("ANA-1", 100);
        LockedWithdrawal one = new LockedWithdrawal("A", shared, "ANA-1", "server-1:ANA-1", 80);
        LockedWithdrawal two = new LockedWithdrawal("B", shared, "ANA-1", "server-2:ANA-1", 80);
        Interleaving.run("ABABABAB", one, two);
        line(out, "  two servers, each with its own lock in its own memory, sharing one database: A " + one.outcome() + ", B " + two.outcome()
                + ", paid out " + shared.paidOut("ANA-1") + " from 100  <- A LOCK IN ONE PROCESS DOES NOT STOP ANOTHER");
        line(out, "  with more than one server, the guarantee must come from the database: row locks, isolation levels or a version check,");
        line(out, "  which is the -with-postgres version of this project");
    }
}
