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
}
