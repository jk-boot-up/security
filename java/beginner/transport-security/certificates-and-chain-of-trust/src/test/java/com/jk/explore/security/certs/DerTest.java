package com.jk.explore.security.certs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DerTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    @Test
    void objectIdentifiersMatchTheirPublishedEncodings() {
        assertArrayEquals(bytes(0x06, 0x03, 0x55, 0x04, 0x03), Der.oid("2.5.4.3"));
        assertArrayEquals(bytes(0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x04, 0x03, 0x02), Der.oid("1.2.840.10045.4.3.2"));
        assertArrayEquals(bytes(0x06, 0x03, 0x55, 0x1d, 0x13), Der.oid("2.5.29.19"));
    }

    @Test
    void integersKeepTheirSignBitClear() {
        assertArrayEquals(bytes(0x02, 0x01, 0x03), Der.integer(3));
        assertArrayEquals(bytes(0x02, 0x02, 0x00, 0x80), Der.integer(128));
        assertArrayEquals(bytes(0x02, 0x01, 0x00), Der.integer(0));
    }

    @Test
    void lengthsUseTheShortAndLongForms() {
        assertArrayEquals(bytes(0x04, 0x02, 1, 2), Der.octets(bytes(1, 2)));
        byte[] hundredTwenty = Der.octets(new byte[127]);
        assertEquals(2 + 127, hundredTwenty.length);
        byte[] long1 = Der.octets(new byte[200]);
        assertArrayEquals(bytes(0x04, 0x81, 200), java.util.Arrays.copyOf(long1, 3));
        byte[] long2 = Der.octets(new byte[300]);
        assertArrayEquals(bytes(0x04, 0x82, 0x01, 0x2c), java.util.Arrays.copyOf(long2, 4));
    }

    @Test
    void timesAreUtcTimeStrings() {
        byte[] time = Der.utcTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertEquals(0x17, time[0]);
        assertEquals("260101000000Z", new String(time, 2, time[1], java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void booleansAndBitStrings() {
        assertArrayEquals(bytes(0x01, 0x01, 0xff), Der.bool(true));
        assertArrayEquals(bytes(0x03, 0x02, 0x01, 0x06), Der.bitString(1, (byte) 0x06));
    }
}
