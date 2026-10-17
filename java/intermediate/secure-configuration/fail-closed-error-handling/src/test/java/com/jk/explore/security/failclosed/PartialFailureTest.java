package com.jk.explore.security.failclosed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartialFailureTest {

    private static Ledger ledger() {
        return new Ledger(Map.of("ana", 100_000L, "ben", 0L, "cara", 0L), Set.of("ben"));
    }

    @Test
    void aNaiveTransferToAFrozenAccountLosesTheMoney() {
        Ledger ledger = ledger();
        assertFalse(Transfers.naive(ledger, "ana", "ben", 25_000));
        assertEquals(75_000, ledger.total());
    }

    @Test
    void anAtomicTransferEitherHappensOrDoesNot() {
        Ledger ledger = ledger();
        assertFalse(Transfers.atomic(ledger, "ana", "ben", 25_000));
        assertEquals(100_000, ledger.total());
        assertTrue(Transfers.atomic(ledger, "ana", "cara", 25_000));
        assertEquals(75_000, ledger.balance("ana"));
        assertEquals(25_000, ledger.balance("cara"));
        assertEquals(100_000, ledger.total());
    }

    @Test
    void anAtomicTransferRefusesBadAmountsAndUnknownAccounts() {
        Ledger ledger = ledger();
        assertFalse(Transfers.atomic(ledger, "ana", "cara", 0));
        assertFalse(Transfers.atomic(ledger, "ana", "cara", -5));
        assertFalse(Transfers.atomic(ledger, "ana", "nobody", 5));
        assertFalse(Transfers.atomic(ledger, "ana", "cara", 100_001));
        assertEquals(100_000, ledger.balance("ana"));
    }

    @Test
    void connectionsLeakWithoutTryWithResources() {
        ConnectionPool pool = new ConnectionPool(2);
        assertThrows(IllegalStateException.class, () -> ConnectionPool.naiveUpdate(pool, true));
        assertEquals(1, pool.free());
        ConnectionPool safe = new ConnectionPool(2);
        assertThrows(IllegalStateException.class, () -> ConnectionPool.safeUpdate(safe, true));
        ConnectionPool.safeUpdate(safe, false);
        assertEquals(2, safe.free());
    }
}
