package com.jk.explore.security.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InvoiceStoreTest {

    @Test
    void anUntouchedInvoiceLooksIntact() {
        InvoiceStore store = new InvoiceStore();
        store.save(IntegrityDemo.INVOICE);
        assertTrue(store.looksIntact("INV-1001"));
    }

    @Test
    void anInvoiceChangedAloneIsNoticed() {
        InvoiceStore store = new InvoiceStore();
        store.save(IntegrityDemo.INVOICE);
        store.tamper(IntegrityDemo.INVOICE.withAmount(12), false);
        assertFalse(store.looksIntact("INV-1001"));
    }

    @Test
    void anInvoiceChangedWithItsHashLooksIntact() {
        InvoiceStore store = new InvoiceStore();
        store.save(IntegrityDemo.INVOICE);
        store.tamper(IntegrityDemo.INVOICE.withAmount(12), true);
        assertTrue(store.looksIntact("INV-1001"));
        assertEquals(12, store.get("INV-1001").amountDollars());
    }

    @Test
    void aHashFromAnotherChannelCatchesATamperedDownload() {
        String published = Sha256.hex("shop-app-installer-v1");
        assertEquals(published, Sha256.hex("shop-app-installer-v1"));
        assertFalse(published.equals(Sha256.hex("shop-app-installer-v1-with-malware")));
    }
}
