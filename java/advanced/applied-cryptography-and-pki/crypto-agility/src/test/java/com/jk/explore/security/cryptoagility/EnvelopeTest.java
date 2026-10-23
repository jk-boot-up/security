package com.jk.explore.security.cryptoagility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnvelopeTest {

    private static final Envelope.Rules ALL = new Envelope.Rules(Set.of(1, 2, 3), 2);
    private final KeyRing keys = new KeyRing();
    private final int k = keys.add();
    private final Envelope envelope = new Envelope(keys);
    private final byte[] plain = "customer-7;balance=100".getBytes(StandardCharsets.UTF_8);

    @Test
    void everyWrittenVersionOpensWithItsHeader() throws Exception {
        for (int v : new int[] {1, 2, 3}) {
            byte[] blob = envelope.seal(plain, v, k);
            assertEquals(v, Envelope.versionOf(blob));
            assertEquals(k, Envelope.keyOf(blob));
            assertArrayEquals(plain, envelope.open(blob, ALL));
        }
    }

    @Test
    void twoSealsOfTheSameTextDiffer() {
        assertFalse(java.util.Arrays.equals(envelope.seal(plain, 2, k), envelope.seal(plain, 2, k)));
    }

    @Test
    void aFlippedBitInTheBodyIsRefused() {
        for (int v : new int[] {1, 2, 3}) {
            byte[] blob = envelope.seal(plain, v, k);
            blob[blob.length - 1] ^= 1;
            assertThrows(GeneralSecurityException.class, () -> envelope.open(blob, ALL));
        }
    }

    @Test
    void aChangedHeaderIsRefusedBecauseTheHeaderIsAuthenticated() {
        byte[] blob = envelope.seal(plain, 2, k);
        byte[] other = blob.clone();
        other[1] = 3;
        assertThrows(GeneralSecurityException.class, () -> envelope.open(other, ALL));
        int k2 = keys.add();
        byte[] rekeyed = blob.clone();
        rekeyed[2] = (byte) k2;
        assertThrows(GeneralSecurityException.class, () -> envelope.open(rekeyed, ALL));
    }

    @Test
    void theHeaderOfAVersionOneRecordIsInItsTagToo() {
        byte[] blob = envelope.seal(plain, 1, k);
        blob[1] = 1;
        blob[2] = (byte) (k + 1);
        assertThrows(GeneralSecurityException.class, () -> envelope.open(blob, ALL));
    }

    @Test
    void thePolicyRefusesAVersionItDoesNotReadAndItNeverWritesTheFirstSuite() {
        byte[] blob = envelope.seal(plain, 1, k);
        assertThrows(GeneralSecurityException.class, () -> envelope.open(blob, new Envelope.Rules(Set.of(2, 3), 3)));
        assertThrows(IllegalArgumentException.class, () -> envelope.seal(plain, Envelope.V0_ECB, k));
    }

    @Test
    void aRetiredKeyCannotOpenWhatItSealed() {
        byte[] blob = envelope.seal(plain, 2, k);
        keys.retire(k);
        assertThrows(GeneralSecurityException.class, () -> envelope.open(blob, ALL));
    }

    @Test
    void derivedKeysDifferByPurposeAndKeyId() {
        int k2 = keys.add();
        assertFalse(java.util.Arrays.equals(keys.derive(k, "gcm", 32), keys.derive(k, "chacha", 32)));
        assertFalse(java.util.Arrays.equals(keys.derive(k, "gcm", 32), keys.derive(k2, "gcm", 32)));
        assertEquals(48, keys.derive(k, "long", 48).length);
        assertTrue(keys.has(k2));
        assertNotEquals(k, k2);
    }
}
