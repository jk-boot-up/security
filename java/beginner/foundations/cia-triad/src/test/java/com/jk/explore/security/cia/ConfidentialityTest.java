package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfidentialityTest {

    private final SecureStore store = new SecureStore(new Seal("test-secret"));

    ConfidentialityTest() {
        store.save(CiaDemo.MAYA);
    }

    @Test
    void supportSeesCustomerAndItemOnly() {
        OrderView view = store.read(CiaDemo.RAJ, "O-101");
        assertEquals("Maya Rao", view.customer());
        assertEquals("Laptop", view.item());
        assertNull(view.address());
        assertNull(view.amountDollars());
    }

    @Test
    void financeSeesEverything() {
        OrderView view = store.read(CiaDemo.ANA, "O-101");
        assertEquals("12 Palm Street", view.address());
        assertEquals(1200, view.amountDollars());
    }

    @Test
    void onlyFinanceMayChangeAnAmount() {
        assertThrows(AccessDeniedException.class, () -> store.setAmount(CiaDemo.MALLORY, "O-101", 12));
        store.setAmount(CiaDemo.ANA, "O-101", 1100);
        assertEquals(1100, store.read(CiaDemo.ANA, "O-101").amountDollars());
    }
}
