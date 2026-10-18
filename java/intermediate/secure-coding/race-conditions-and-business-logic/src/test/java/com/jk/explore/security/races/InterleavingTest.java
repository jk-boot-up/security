package com.jk.explore.security.races;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

/** Every possible order of steps for two requests, not just the one the demo prints. */
class InterleavingTest {

    /** Every string of A and B of the given length. */
    private static List<String> allOrders(int length) {
        List<String> orders = new ArrayList<>();
        for (int bits = 0; bits < (1 << length); bits++) {
            StringBuilder order = new StringBuilder();
            for (int i = 0; i < length; i++) {
                order.append((bits >> i & 1) == 0 ? 'A' : 'B');
            }
            orders.add(order.toString());
        }
        return orders;
    }

    private static int overdrawingOrders(BiFunction<Ledger, String, Steps> withdrawal) {
        int bad = 0;
        for (String order : allOrders(8)) {
            Ledger ledger = new Ledger().open("ANA-1", 100);
            Interleaving.run(order, withdrawal.apply(ledger, "A"), withdrawal.apply(ledger, "B"));
            if (ledger.paidOut("ANA-1") > 100 || ledger.balance("ANA-1") < 0 || ledger.balance("ANA-1") + ledger.paidOut("ANA-1") != 100) {
                bad++;
            }
        }
        return bad;
    }

    @Test
    void checkThenActOverdrawsInSomeOrdersAndNotInOthers() {
        int bad = overdrawingOrders((ledger, name) -> new NaiveWithdrawal(name, ledger, "ANA-1", 80));
        assertTrue(bad > 0);
        assertTrue(bad < 256);
        Ledger inTurn = new Ledger().open("ANA-1", 100);
        Interleaving.run("AAABBB", new NaiveWithdrawal("A", inTurn, "ANA-1", 80), new NaiveWithdrawal("B", inTurn, "ANA-1", 80));
        assertEquals(80, inTurn.paidOut("ANA-1"));
    }

    @Test
    void aLockKeepsTheBalanceRightInEveryOrder() {
        assertEquals(0, overdrawingOrders((ledger, name) -> new LockedWithdrawal(name, ledger, "ANA-1", "ANA-1", 80)));
    }

    @Test
    void compareAndSetKeepsTheBalanceRightInEveryOrder() {
        assertEquals(0, overdrawingOrders((ledger, name) -> new CasWithdrawal(name, ledger, "ANA-1", 80)));
    }

    @Test
    void locksInSeparateProcessesDoNotProtectSharedData() {
        int bad = overdrawingOrders((ledger, name) -> new LockedWithdrawal(name, ledger, "ANA-1", "server-" + name + ":ANA-1", 80));
        assertTrue(bad > 0);
    }

    @Test
    void aCouponClaimedInOneStepIsUsedOnceInEveryOrder() {
        for (String order : allOrders(6)) {
            CouponBook book = new CouponBook();
            Interleaving.run(order, new CouponRedemption("A", book, "SAVE20", 20, true), new CouponRedemption("B", book, "SAVE20", 20, true));
            assertEquals(20, book.discountGiven(), order);
        }
        CouponBook racy = new CouponBook();
        Interleaving.run("ABABAB", new CouponRedemption("A", racy, "SAVE20", 20, false), new CouponRedemption("B", racy, "SAVE20", 20, false));
        assertEquals(40, racy.discountGiven());
    }

    @Test
    void locksTakenInOppositeOrdersCanDeadlock() {
        Ledger ledger = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
        Interleaving.Result result = Interleaving.run("AB",
                new LockingTransfer("A", ledger, "ANA-1", "BEN-2", 50, false),
                new LockingTransfer("B", ledger, "BEN-2", "ANA-1", 70, false));
        assertTrue(result.deadlock());
    }

    @Test
    void locksTakenInOneFixedOrderNeverDeadlockAndKeepTheMoney() {
        for (String order : allOrders(6)) {
            Ledger ledger = new Ledger().open("ANA-1", 500).open("BEN-2", 500);
            Interleaving.Result result = Interleaving.run(order,
                    new LockingTransfer("A", ledger, "ANA-1", "BEN-2", 50, true),
                    new LockingTransfer("B", ledger, "BEN-2", "ANA-1", 70, true));
            assertFalse(result.deadlock(), order);
            assertEquals(1000, ledger.balance("ANA-1") + ledger.balance("BEN-2"), order);
            assertEquals(520, ledger.balance("ANA-1"), order);
        }
    }
}
