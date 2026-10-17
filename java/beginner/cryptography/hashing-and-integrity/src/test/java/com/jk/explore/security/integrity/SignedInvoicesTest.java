package com.jk.explore.security.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SignedInvoicesTest {

    private final SignedInvoices.Encoding joined = SignedInvoices.Encoding.JOINED;
    private final SignedInvoices.Encoding prefixed = SignedInvoices.Encoding.LENGTH_PREFIXED;

    @Test
    void aRightTagIsAccepted() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        assertEquals(SignedInvoices.Verdict.OK, s.accept(IntegrityDemo.INVOICE, s.tag(IntegrityDemo.INVOICE)));
    }

    @Test
    void aChangedInvoiceWithTheOldTagIsRefused() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        String tag = s.tag(IntegrityDemo.INVOICE);
        assertEquals(SignedInvoices.Verdict.BAD_TAG, s.accept(IntegrityDemo.INVOICE.withAmount(12), tag));
    }

    @Test
    void aPlainHashIsNotAnAcceptableTag() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        Invoice forged = IntegrityDemo.INVOICE.withAmount(12);
        assertEquals(SignedInvoices.Verdict.BAD_TAG, s.accept(forged, Sha256.hex(forged.text())));
    }

    @Test
    void aTagMadeWithAnotherKeyIsRefused() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        SignedInvoices attacker = new SignedInvoices(Hex.bytes("a guess"), prefixed, false);
        Invoice forged = IntegrityDemo.INVOICE.withAmount(12);
        assertEquals(SignedInvoices.Verdict.BAD_TAG, s.accept(forged, attacker.tag(forged)));
    }

    @Test
    void joinedFieldsLetABoundaryMoveWithoutChangingTheTag() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, joined, false);
        Invoice a = new Invoice("INV-7", "ab", "c", 5);
        Invoice b = new Invoice("INV-7", "a", "bc", 5);
        assertEquals(s.tag(a), s.tag(b));
        assertEquals(SignedInvoices.Verdict.OK, s.accept(b, s.tag(a)));
    }

    @Test
    void lengthPrefixedFieldsGiveDifferentTagsWhenABoundaryMoves() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        Invoice a = new Invoice("INV-7", "ab", "c", 5);
        Invoice b = new Invoice("INV-7", "a", "bc", 5);
        assertNotEquals(s.tag(a), s.tag(b));
        assertEquals(SignedInvoices.Verdict.BAD_TAG, s.accept(b, s.tag(a)));
    }

    @Test
    void aTagAloneDoesNotStopAReplay() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        String tag = s.tag(IntegrityDemo.INVOICE);
        assertEquals(SignedInvoices.Verdict.OK, s.accept(IntegrityDemo.INVOICE, tag));
        assertEquals(SignedInvoices.Verdict.OK, s.accept(IntegrityDemo.INVOICE, tag));
    }

    @Test
    void rememberingInvoiceNumbersStopsAReplay() {
        SignedInvoices s = new SignedInvoices(IntegrityDemo.KEY, prefixed, true);
        String tag = s.tag(IntegrityDemo.INVOICE);
        assertEquals(SignedInvoices.Verdict.OK, s.accept(IntegrityDemo.INVOICE, tag));
        assertEquals(SignedInvoices.Verdict.REPLAYED, s.accept(IntegrityDemo.INVOICE, tag));
    }

    @Test
    void anyHolderOfTheKeyCanMakeATheServiceAccepts() {
        SignedInvoices checker = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        SignedInvoices holder = new SignedInvoices(IntegrityDemo.KEY, prefixed, false);
        Invoice made = new Invoice("INV-9000", "mallory", "Laptop", 1);
        assertEquals(SignedInvoices.Verdict.OK, checker.accept(made, holder.tag(made)));
    }
}
