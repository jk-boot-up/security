package com.jk.explore.security.passkeysspring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The CBOR that an attestation object and a COSE key are made of, against the bytes the specification gives. */
class CborTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    @Test
    void smallIntegersAreOneByteAndNegativesCountDown() {
        assertArrayEquals(bytes(0xa2, 0x01, 0x02, 0x03, 0x26), Cbor.map(1, 2, 3, -7));
        assertArrayEquals(bytes(0xa1, 0x20, 0x01), Cbor.map(-1, 1));
    }

    @Test
    void textStringsCarryTheirLength() {
        assertArrayEquals(bytes(0xa1, 0x63, 'f', 'm', 't', 0x64, 'n', 'o', 'n', 'e'), Cbor.map("fmt", "none"));
    }

    @Test
    void byteStringsCarryTheirLengthAndLongOnesUseTwoBytesOfLength() {
        assertArrayEquals(bytes(0xa1, 0x01, 0x42, 0xaa, 0xbb), Cbor.map(1, new byte[] {(byte) 0xaa, (byte) 0xbb}));
        byte[] long1 = new byte[300];
        byte[] encoded = Cbor.map(1, long1);
        assertArrayEquals(bytes(0xa1, 0x01, 0x59, 0x01, 0x2c), java.util.Arrays.copyOf(encoded, 5));
    }

    @Test
    void anEmptyMapIsOneByte() {
        assertArrayEquals(bytes(0xa1, 0x67, 'a', 't', 't', 'S', 't', 'm', 't', 0xa0), Cbor.map("attStmt", Map.of()));
    }

    @Test
    void anUnsupportedItemIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Cbor.map(1, 2.5));
    }
}
