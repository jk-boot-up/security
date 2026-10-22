package com.jk.explore.security.tlscapture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The capture file writer, read back byte by byte: no program is needed. */
class PcapTest {

    @Test
    void aConnectionIsAHandshakeThePayloadInSegmentsAndAnEnd() throws Exception {
        Path file = Files.createTempFile("pcap-test", ".pcap");
        byte[] payload = new byte[3000];
        try (Pcap pcap = new Pcap(file, 50000, 443)) {
            pcap.data(true, payload, payload.length);
        }
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0xa1b2c3d4, b.getInt());
        b.position(20);
        assertEquals(101, b.getInt());
        int packets = 0;
        int dataBytes = 0;
        while (b.hasRemaining()) {
            b.position(b.position() + 8);
            int length = b.getInt();
            b.getInt();
            byte[] ip = new byte[length];
            b.get(ip);
            assertEquals(0x45, ip[0]);
            assertEquals(6, ip[9]);
            assertEquals(0, checksum(ip, 0, 20), "the IP header checksum adds up");
            dataBytes += length - 40;
            packets++;
        }
        assertEquals(3 + 3 + 3, packets, "three to open, three of 1400, 1400 and 200 bytes, three to close");
        assertEquals(3000, dataBytes);
    }

    @Test
    void sequenceNumbersFollowTheBytesSent() throws Exception {
        Path file = Files.createTempFile("pcap-test", ".pcap");
        try (Pcap pcap = new Pcap(file, 50001, 8443)) {
            pcap.data(true, new byte[100], 100);
            pcap.data(false, new byte[40], 40);
        }
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        b.position(24);
        long firstData = -1;
        long reply = -1;
        long replyAck = -1;
        while (b.hasRemaining()) {
            b.position(b.position() + 8);
            int length = b.getInt();
            b.getInt();
            ByteBuffer ip = ByteBuffer.wrap(new byte[length]);
            b.get(ip.array());
            if (length == 140 && firstData < 0) {
                firstData = Integer.toUnsignedLong(ip.getInt(24));
            }
            if (length == 80) {
                reply = Integer.toUnsignedLong(ip.getInt(24));
                replyAck = Integer.toUnsignedLong(ip.getInt(28));
            }
        }
        assertEquals(1001, firstData);
        assertEquals(5001, reply);
        assertEquals(1101, replyAck, "the server acknowledges the 100 bytes");
        assertTrue(reply > 0);
    }

    private static int checksum(byte[] data, int from, int length) {
        long sum = 0;
        for (int i = from; i < from + length; i += 2) {
            sum += ((data[i] & 0xff) << 8) | (data[i + 1] & 0xff);
        }
        while (sum >> 16 != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        return (int) (~sum & 0xffff);
    }
}
