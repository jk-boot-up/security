package com.jk.explore.security.tcpipdump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureTest {

    @Test
    void checksumMatchesTheRfc1071Example() {
        byte[] data = {0x00, 0x01, (byte) 0xf2, 0x03, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7};
        assertEquals(0x220d, Checksum.of(data));
    }

    @Test
    void pcapFileHasTheDocumentedLayout() {
        byte[] file = PcapFile.write(List.of(new byte[] {1, 2, 3}));
        assertEquals(24 + 16 + 3, file.length);
        ByteBuffer buffer = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0xa1b2c3d4, buffer.getInt());
        assertEquals(2, buffer.getShort());
        assertEquals(4, buffer.getShort());
        buffer.position(16);
        assertEquals(65535, buffer.getInt());
        assertEquals(101, buffer.getInt());
        assertEquals(PcapFile.FIRST_SECOND, buffer.getInt() & 0xffffffffL);
    }

    @Test
    void pcapRoundTrips() throws Exception {
        List<byte[]> packets = Traffic.everything();
        List<byte[]> back = PcapFile.read(PcapFile.write(packets));
        assertEquals(packets.size(), back.size());
        for (int i = 0; i < packets.size(); i++) {
            assertArrayEquals(packets.get(i), back.get(i));
        }
    }

    @Test
    void badFilesAreRefused() {
        assertThrows(BadPacketException.class, () -> PcapFile.read(new byte[10]));
        assertThrows(BadPacketException.class, () -> PcapFile.read(new byte[24]));
        byte[] cut = PcapFile.write(List.of(new byte[] {1, 2, 3}));
        byte[] shorter = java.util.Arrays.copyOf(cut, cut.length - 1);
        assertThrows(BadPacketException.class, () -> PcapFile.read(shorter));
    }

    @Test
    void linesDescribeTheHandshakeAndTheData() throws Exception {
        List<String> lines = Capture.lines(PcapFile.write(Traffic.purchase()));
        assertEquals("IP 198.51.100.7.51000 > 203.0.113.10.443: Flags [S], seq 1000, win 65535, length 0", lines.get(0));
        assertEquals("IP 203.0.113.10.443 > 198.51.100.7.51000: Flags [S.], seq 7000, ack 1001, win 65535, length 0", lines.get(1));
        assertEquals("IP 198.51.100.7.51000 > 203.0.113.10.443: Flags [P.], seq 1001:1035, ack 7001, win 65535, length 34", lines.get(3));
    }

    @Test
    void floodSourcesAreFoundAndTheCustomerIsNot() throws Exception {
        var unfinished = Capture.unfinishedHandshakes(PcapFile.write(Traffic.everything()));
        assertEquals(9, unfinished.size());
        assertTrue(unfinished.contains("100.64.0.1"));
        assertFalse(unfinished.contains("198.51.100.7"));
    }

    @Test
    void onlyTheDamagedPacketFailsItsChecksum() throws Exception {
        assertEquals(0, Capture.damagedCount(PcapFile.write(Traffic.purchase())));
        assertEquals(1, Capture.damagedCount(PcapFile.write(Traffic.everything())));
    }

    @Test
    void theCaptureHoldsTheCardNumber() throws Exception {
        assertTrue(Capture.containsText(PcapFile.write(Traffic.purchase()), "4111111111111111"));
        assertFalse(Capture.containsText(PcapFile.write(Traffic.flood()), "4111111111111111"));
    }
}
