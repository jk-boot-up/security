package com.jk.explore.security.cryptoagility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StoreAndInventoryTest {

    private final KeyRing keys = new KeyRing();
    private final Envelope envelope = new Envelope(keys);
    private final Store store = new Store(envelope);
    private final Envelope.Rules rules = new Envelope.Rules(Set.of(1, 2, 3), 2);

    @Test
    void aReadWithRepairRewritesTheRecordInTheCurrentSuiteAndKey() throws Exception {
        int k1 = keys.add();
        store.put(1, 1, k1);
        int k2 = keys.add();
        assertEquals(Store.plainOf(1), store.read(1, rules, k2, true));
        assertEquals(Map.of("v2/key" + k2, 1), store.census());
    }

    @Test
    void aReadWithoutRepairChangesNothing() throws Exception {
        int k1 = keys.add();
        store.put(1, 1, k1);
        store.read(1, rules, k1 + 1, false);
        assertEquals(Map.of("v1/key" + k1, 1), store.census());
    }

    @Test
    void aBatchMovesEveryRecordAndKeepsTheText() throws Exception {
        int k1 = keys.add();
        for (int i = 1; i <= 50; i++) {
            store.put(i, 1, k1);
        }
        int k2 = keys.add();
        assertEquals(50, store.migrateAll(rules, k2));
        assertEquals(0, store.migrateAll(rules, k2));
        assertEquals(50, store.intact(rules));
        assertEquals(0, store.count(k1));
    }

    @Test
    void recordsUnderARetiredKeyAreNotIntact() {
        int k1 = keys.add();
        store.put(1, 2, k1);
        keys.retire(k1);
        assertEquals(0, store.intact(rules));
    }

    @Test
    void theInventoryFindsTwentyUsesInTenFilesWithTheirStatus() throws Exception {
        List<Inventory.Use> uses = Inventory.scan(Path.of("fixtures/bank-src"));
        assertEquals(20, uses.size());
        assertEquals(10, uses.stream().map(Inventory.Use::file).distinct().count());
        assertEquals(0, uses.stream().filter(u -> u.status() == Status.UNKNOWN).count());
    }

    @Test
    void thePolicyKnowsApprovedDeprecatedForbiddenAndUnknown() {
        assertEquals(Status.APPROVED, Policy.of("AES/GCM/NoPadding"));
        assertEquals(Status.DEPRECATED, Policy.of("SHA-1"));
        assertEquals(Status.FORBIDDEN, Policy.of("MD5"));
        assertTrue(Policy.of("Rot13") == Status.UNKNOWN);
    }
}
