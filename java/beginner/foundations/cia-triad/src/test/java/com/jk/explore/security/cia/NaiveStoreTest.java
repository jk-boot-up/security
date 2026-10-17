package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NaiveStoreTest {

    private final NaiveStore store = new NaiveStore();

    NaiveStoreTest() {
        store.save(CiaDemo.MAYA);
    }

    @Test
    void anyoneReadsTheShippingAddress() {
        assertEquals("12 Palm Street", store.read(CiaDemo.RAJ, "O-101").address());
    }

    @Test
    void anyoneChangesTheAmountAndNothingNotices() {
        store.setAmount(CiaDemo.MALLORY, "O-101", 12);
        assertEquals(12, store.read(CiaDemo.ANA, "O-101").amountDollars());
    }

    @Test
    void aFloodTakesTheWholeServer() {
        TickServer.Result result = new TickServer(CiaDemo.SERVER_CAPACITY).serve(CiaDemo.flood(), null);
        assertEquals(10, result.totalServed());
        assertEquals(10, result.served().get("bot"));
        assertNull(result.served().get("maya"), "maya is never served");
        assertEquals(1, result.refused().get("maya"));
    }
}
