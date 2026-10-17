package com.jk.explore.security.integrity;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores invoices, each with a plain SHA-256 hash of its text kept right beside it. The hash is meant to show whether the
 * invoice was changed, but an attacker who can write the invoice can write the hash as well.
 */
public class InvoiceStore {

    private final Map<String, Invoice> invoices = new HashMap<>();
    private final Map<String, String> hashes = new HashMap<>();

    public void save(Invoice invoice) {
        invoices.put(invoice.number(), invoice);
        hashes.put(invoice.number(), Sha256.hex(invoice.text()));
    }

    /** Standing for an attacker who can reach the database: replaces the invoice, and the hash beside it if asked to. */
    public void tamper(Invoice changed, boolean fixTheHashToo) {
        invoices.put(changed.number(), changed);
        if (fixTheHashToo) {
            hashes.put(changed.number(), Sha256.hex(changed.text()));
        }
    }

    /** True if the stored hash matches the stored invoice. */
    public boolean looksIntact(String number) {
        return hashes.get(number).equals(Sha256.hex(invoices.get(number).text()));
    }

    public Invoice get(String number) {
        return invoices.get(number);
    }
}
