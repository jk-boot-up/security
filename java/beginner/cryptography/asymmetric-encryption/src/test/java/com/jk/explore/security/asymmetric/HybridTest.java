package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HybridTest {

    private static KeyPair ana;
    private static KeyPair eve;
    private final byte[] aad = AsymmetricDemo.ACCOUNT;

    @BeforeAll
    static void keys() {
        ana = RsaBox.newKeyPair();
        eve = RsaBox.newKeyPair();
    }

    @Test
    void aLargeMessageSealsAndOpens() {
        byte[] big = new byte[100_000];
        Arrays.fill(big, (byte) 7);
        assertArrayEquals(big, HybridBox.open(ana.getPrivate(), aad, HybridBox.seal(ana.getPublic(), aad, big)));
    }

    @Test
    void theEnvelopeIsLockedKeyPlusNoncePlusMessagePlusTag() {
        Envelope e = HybridBox.seal(ana.getPublic(), aad, new byte[10_000]);
        assertEquals(256, e.lockedKey().length);
        assertEquals(12, e.nonce().length);
        assertEquals(10_016, e.ciphertextWithTag().length);
        assertEquals(10_284, e.totalLength());
    }

    @Test
    void everyMessageGetsItsOwnDataKey() {
        Envelope a = HybridBox.seal(ana.getPublic(), aad, Hex.bytes("same"));
        Envelope b = HybridBox.seal(ana.getPublic(), aad, Hex.bytes("same"));
        assertFalse(Arrays.equals(a.lockedKey(), b.lockedKey()));
        assertFalse(Arrays.equals(a.ciphertextWithTag(), b.ciphertextWithTag()));
    }

    @Test
    void anotherPrivateKeyCannotOpenIt() {
        Envelope e = HybridBox.seal(ana.getPublic(), aad, Hex.bytes("secret"));
        assertThrows(RsaBox.NotForThisKeyException.class, () -> HybridBox.open(eve.getPrivate(), aad, e));
    }

    @Test
    void aChangedBitOrOtherAssociatedDataIsRefused() {
        Envelope e = HybridBox.seal(ana.getPublic(), aad, Hex.bytes("secret"));
        byte[] flipped = e.ciphertextWithTag().clone();
        flipped[0] ^= 1;
        assertThrows(RsaBox.NotForThisKeyException.class,
                () -> HybridBox.open(ana.getPrivate(), aad, new Envelope(e.lockedKey(), e.nonce(), flipped)));
        assertThrows(RsaBox.NotForThisKeyException.class, () -> HybridBox.open(ana.getPrivate(), Hex.bytes("account=MAL-2"), e));
    }

    @Test
    void aSwappedPublicKeyLetsAMiddleManReadAndForward() {
        KeyPair mallory = RsaBox.newKeyPair();
        Envelope toMallory = HybridBox.seal(mallory.getPublic(), aad, Hex.bytes("PIN is 4821"));
        byte[] read = HybridBox.open(mallory.getPrivate(), aad, toMallory);
        Envelope onward = HybridBox.seal(ana.getPublic(), aad, read);
        assertEquals("PIN is 4821", Hex.text(HybridBox.open(ana.getPrivate(), aad, onward)));
    }
}
