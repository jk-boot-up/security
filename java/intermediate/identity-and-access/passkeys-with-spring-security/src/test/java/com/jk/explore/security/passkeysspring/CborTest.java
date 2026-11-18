package com.jk.explore.security.passkeysspring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void aCoseKeyReadsBackAsItsLabelsAndValues() {
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        java.util.List<Object[]> pairs = Cbor.readMap(Cbor.map(1, 2, 3, -7, -1, 1, -2, x, -3, y));
        assertEquals(java.util.List.of(1, 3, -1, -2, -3), pairs.stream().map(p -> p[0]).toList());
        assertEquals(java.util.List.of(2, -7, 1), pairs.subList(0, 3).stream().map(p -> p[1]).toList());
        assertEquals(32, ((byte[]) pairs.get(3)[1]).length);
    }

    @Test
    void theSignedBytesAreTheAuthenticatorDataThenTheClientDataHash() throws Exception {
        byte[] authData = new byte[37];
        byte[] clientData = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] signed = SoftwareAuthenticator.signedBytes(authData, clientData);
        assertEquals(69, signed.length);
        assertArrayEquals(java.security.MessageDigest.getInstance("SHA-256").digest(clientData),
                java.util.Arrays.copyOfRange(signed, 37, 69));
    }
}
