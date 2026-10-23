package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class HybridTest {

    @Test
    void bothSidesDeriveTheSameKey() throws Exception {
        Hybrid.Exchange h = Hybrid.run();
        assertArrayEquals(h.clientKey(), h.serverKey());
        assertEquals(32, h.clientKey().length);
    }

    @Test
    void breakingOnlyOnePartDoesNotGiveTheKey() throws Exception {
        Hybrid.Exchange h = Hybrid.run();
        byte[] withoutKem = Hybrid.combine(h.classicalSecret(), new byte[32], h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        byte[] withoutX = Hybrid.combine(new byte[32], h.kemSecret(), h.clientX(), h.clientK(), h.serverX(), h.ciphertext());
        assertFalse(Hybrid.same(withoutKem, h.clientKey()));
        assertFalse(Hybrid.same(withoutX, h.clientKey()));
    }

    @Test
    void aChangedTranscriptChangesTheKey() throws Exception {
        Hybrid.Exchange h = Hybrid.run();
        byte[] ct = h.ciphertext().clone();
        ct[0] ^= 1;
        assertFalse(Hybrid.same(Hybrid.combine(h.classicalSecret(), h.kemSecret(), h.clientX(), h.clientK(), h.serverX(), ct), h.clientKey()));
    }

    @Test
    void twoExchangesGiveDifferentKeys() throws Exception {
        assertFalse(Hybrid.same(Hybrid.run().clientKey(), Hybrid.run().clientKey()));
    }
}
