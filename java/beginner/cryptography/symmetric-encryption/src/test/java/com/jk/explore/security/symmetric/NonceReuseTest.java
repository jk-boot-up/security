package com.jk.explore.security.symmetric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NonceReuseTest {

    private final byte[] aad = Hex.bytes("account=ANA-1");
    private final byte[] known = Hex.bytes("balance=000100");
    private final byte[] secret = Hex.bytes("balance=900100");

    @Test
    void twoCipherTextsMadeWithTheSameNonceXorToTheXorOfThePlainTexts() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new RepeatedNonce(new byte[12]));
        byte[] c1 = box.seal(aad, known).ciphertext();
        byte[] c2 = box.seal(aad, secret).ciphertext();
        assertArrayEquals(Hex.xor(known, secret, known.length), Hex.xor(c1, c2, known.length));
    }

    @Test
    void anAttackerWhoKnowsOneMessageRecoversTheOther() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new RepeatedNonce(new byte[12]));
        byte[] c1 = box.seal(aad, known).ciphertext();
        byte[] c2 = box.seal(aad, secret).ciphertext();
        byte[] recovered = Hex.xor(Hex.xor(c1, c2, known.length), known, known.length);
        assertEquals("balance=900100", Hex.text(recovered));
    }

    @Test
    void withFreshNoncesTheSameTrickGivesNothing() {
        GcmBox box = new GcmBox(SymmetricDemo.KEY, new CountingNonces());
        byte[] c1 = box.seal(aad, known).ciphertext();
        byte[] c2 = box.seal(aad, secret).ciphertext();
        byte[] notRecovered = Hex.xor(Hex.xor(c1, c2, known.length), known, known.length);
        org.junit.jupiter.api.Assertions.assertNotEquals("balance=900100", Hex.text(notRecovered));
    }
}
