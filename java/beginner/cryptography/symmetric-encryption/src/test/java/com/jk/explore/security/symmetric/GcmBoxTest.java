package com.jk.explore.security.symmetric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GcmBoxTest {

    private final byte[] aad = Hex.bytes("account=ANA-1");
    private final byte[] plain = Hex.bytes("RENT      -1200\n");

    @Test
    void whatIsSealedOpensAgain() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        assertArrayEquals(plain, box.open(aad, box.seal(aad, plain)));
    }

    @Test
    void theSamePlainTextSealedTwiceGivesDifferentCipherTexts() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        assertNotEquals(Hex.of(box.seal(aad, plain).ciphertext()), Hex.of(box.seal(aad, plain).ciphertext()));
    }

    @Test
    void aFlippedBitIsDetected() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        Sealed s = box.seal(aad, plain);
        byte[] bad = s.ciphertextWithTag().clone();
        bad[0] ^= 1;
        assertThrows(GcmBox.TamperedException.class, () -> box.open(aad, new Sealed(s.nonce(), bad)));
    }

    @Test
    void aChangedTagIsDetected() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        Sealed s = box.seal(aad, plain);
        byte[] bad = s.ciphertextWithTag().clone();
        bad[bad.length - 1] ^= 1;
        assertThrows(GcmBox.TamperedException.class, () -> box.open(aad, new Sealed(s.nonce(), bad)));
    }

    @Test
    void aChangedNonceIsDetected() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        Sealed s = box.seal(aad, plain);
        byte[] other = s.nonce().clone();
        other[11] ^= 1;
        assertThrows(GcmBox.TamperedException.class, () -> box.open(aad, new Sealed(other, s.ciphertextWithTag())));
    }

    @Test
    void differentAssociatedDataIsDetected() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        Sealed s = box.seal(aad, plain);
        assertThrows(GcmBox.TamperedException.class, () -> box.open(Hex.bytes("account=MAL-2"), s));
    }

    @Test
    void aDifferentKeyCannotOpenIt() {
        Sealed s = new GcmBox(SymmetricDemo.KEY, new CountingNonces()).seal(aad, plain);
        assertThrows(GcmBox.TamperedException.class,
                () -> new GcmBox(Hex.bytes("another-key-16by"), new CountingNonces()).open(aad, s));
    }

    @Test
    void sealingAddsTwelveBytesOfNonceAndSixteenOfTag() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        assertEquals(5 + 28, box.seal(aad, Hex.bytes("hello")).storedLength());
        assertEquals(500 + 28, box.seal(aad, Hex.bytes("x".repeat(500))).storedLength());
        assertEquals(28, box.seal(aad, new byte[0]).storedLength());
    }

    @Test
    void randomNoncesAreTwelveBytesAndDiffer() {
        RandomNonces nonces = new RandomNonces();
        byte[] a = nonces.next();
        assertEquals(12, a.length);
        assertNotEquals(Arrays.toString(a), Arrays.toString(nonces.next()));
    }

    @Test
    void countingNoncesDifferEachTimeButRepeatAcrossInstances() {
        CountingNonces a = new CountingNonces();
        CountingNonces b = new CountingNonces();
        byte[] first = a.next();
        assertNotEquals(Arrays.toString(first), Arrays.toString(a.next()));
        assertEquals(Arrays.toString(first), Arrays.toString(b.next()));
    }
}
