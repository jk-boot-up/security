package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditLogTest {

    private static AuditLog three() {
        AuditLog log = new AuditLog();
        log.append("signed", "A", "a".getBytes(StandardCharsets.UTF_8));
        log.append("refused-rate", "B", "b".getBytes(StandardCharsets.UTF_8));
        log.append("signed", "C", "c".getBytes(StandardCharsets.UTF_8));
        return log;
    }

    @Test
    void anUntouchedChainVerifies() {
        AuditLog log = three();
        assertTrue(log.verify());
        assertEquals(2, log.count("signed"));
        assertTrue(AuditLog.verify(List.of()));
    }

    @Test
    void anEditedEntryBreaksTheChain() {
        List<AuditLog.Entry> list = new ArrayList<>(three().entries());
        AuditLog.Entry e = list.get(1);
        list.set(1, new AuditLog.Entry(e.seq(), e.event(), "X", e.contentHash(), e.previous(), e.hash()));
        assertFalse(AuditLog.verify(list));
    }

    @Test
    void aRemovedOrReorderedEntryBreaksTheChain() {
        List<AuditLog.Entry> removed = new ArrayList<>(three().entries());
        removed.remove(1);
        assertFalse(AuditLog.verify(removed));
        List<AuditLog.Entry> swapped = new ArrayList<>(three().entries());
        java.util.Collections.swap(swapped, 0, 2);
        assertFalse(AuditLog.verify(swapped));
    }

    @Test
    void theHashIsOfTheContent() {
        assertEquals(AuditLog.sha256("a".getBytes(StandardCharsets.UTF_8)), three().entries().getFirst().contentHash());
    }

    /** The worked example the demo prints: entry 1 of the bank's log, redone by hand with SHA-256. No token needed. */
    @Test
    void theFirstEntryOfTheDemoIsTheHashOfItsFieldsJoinedByBars() {
        AuditLog log = new AuditLog();
        byte[] batch0 = PaymentFile.content("BATCH-0", 5, 150_000L);
        log.append("signed", "BATCH-0", batch0);
        String byHand = AuditLog.sha256(("1|signed|BATCH-0|" + AuditLog.sha256(batch0) + "|genesis").getBytes(StandardCharsets.UTF_8));
        assertEquals(byHand, log.entries().getFirst().hash());
        assertEquals("eeef9a22d1f6", byHand.substring(0, 12));
    }

    @Test
    void eachEntryPointsAtTheHashOfTheOneBefore() {
        List<AuditLog.Entry> e = three().entries();
        assertEquals("genesis", e.get(0).previous());
        assertEquals(e.get(0).hash(), e.get(1).previous());
        assertEquals(e.get(1).hash(), e.get(2).previous());
    }
}
