package com.jk.explore.security.symmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The AES and GCM code against published test values. */
class StandardVectorsTest {

    private static byte[] hex(String h) {
        return HexFormat.of().parseHex(h);
    }

    @Test
    void aesEcbMatchesFips197() {
        byte[] key = hex("000102030405060708090a0b0c0d0e0f");
        byte[] plain = hex("00112233445566778899aabbccddeeff");
        assertEquals("69c4e0d86a7b0430d8cdb78070b4c55a", Hex.of(EcbCipher.encrypt(key, plain)));
    }

    @Test
    void gcmMatchesTestCaseOneOfTheOriginalSpecification() {
        GcmBox box = new GcmBox(new byte[16], new RepeatedNonce(new byte[12]));
        Sealed sealed = box.seal(new byte[0], new byte[0]);
        assertEquals("58e2fccefa7e3061367f1d57a4e7455a", Hex.of(sealed.ciphertextWithTag()));
    }

    @Test
    void gcmMatchesTestCaseTwoOfTheOriginalSpecification() {
        GcmBox box = new GcmBox(new byte[16], new RepeatedNonce(new byte[12]));
        Sealed sealed = box.seal(new byte[0], new byte[16]);
        assertEquals("0388dace60b6a392f328c2b971b2fe78", Hex.of(sealed.ciphertext()));
        assertEquals("ab6e47d42cec13bdf53a67b21257bddf", Hex.of(java.util.Arrays.copyOfRange(sealed.ciphertextWithTag(), 16, 32)));
    }
}
