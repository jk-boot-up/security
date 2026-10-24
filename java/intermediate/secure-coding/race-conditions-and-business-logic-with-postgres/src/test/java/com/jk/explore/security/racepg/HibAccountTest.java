package com.jk.explore.security.racepg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The withdrawal rule on one loaded account is plain in-memory logic; only the concurrent version races on Postgres (that part is integration-tagged). */
class HibAccountTest {

    @Test
    void aFreshAccountKnowsItsFields() {
        HibAccount account = new HibAccount("ANA-1", 100);
        assertEquals("ANA-1", account.id());
        assertEquals(100, account.balance());
        assertEquals(0, account.version(), "a never-persisted entity is at version 0");
    }

    @Test
    void withdrawTakesTheMoneyWhenThereIsEnough() {
        HibAccount account = new HibAccount("ANA-1", 100);
        assertTrue(account.withdraw(40));
        assertEquals(60, account.balance());
    }

    @Test
    void withdrawRefusesAndLeavesTheBalanceWhenThereIsNotEnough() {
        HibAccount account = new HibAccount("ANA-1", 100);
        account.withdraw(40);
        assertFalse(account.withdraw(100), "60 left is not enough for 100");
        assertEquals(60, account.balance(), "a refused withdrawal changes nothing");
        assertTrue(account.withdraw(60));
        assertEquals(0, account.balance());
        assertFalse(account.withdraw(1), "nothing left");
    }
}
