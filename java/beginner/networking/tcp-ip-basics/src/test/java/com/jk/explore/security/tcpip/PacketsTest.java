package com.jk.explore.security.tcpip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PacketsTest {

    @Test
    void checksumMatchesTheRfc1071Example() {
        byte[] data = {0x00, 0x01, (byte) 0xf2, 0x03, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7};
        assertEquals(0xddf2, Checksum.sum(data));
        assertEquals(0x220d, Checksum.of(data));
    }

    @Test
    void checksumHandlesAnOddLength() {
        assertEquals(Checksum.of(new byte[] {1, 2, 3, 0}), Checksum.of(new byte[] {1, 2, 3}));
    }

    @Test
    void addressesRoundTripAndRejectNonsense() {
        assertEquals("198.51.100.7", Address.text(Address.parse("198.51.100.7")));
        assertThrows(IllegalArgumentException.class, () -> Address.parse("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> Address.parse("1.2.3.256"));
    }

    @Test
    void ipv4PacketRoundTrips() throws Exception {
        Ipv4Packet packet = new Ipv4Packet(Address.parse("10.0.0.1"), Address.parse("10.0.0.2"), 64, new byte[] {9, 8, 7});
        byte[] bytes = packet.encode();
        assertEquals(23, bytes.length);
        Ipv4Packet decoded = Ipv4Packet.decode(bytes);
        assertEquals(packet.source(), decoded.source());
        assertEquals(packet.destination(), decoded.destination());
        assertEquals(64, decoded.timeToLive());
        assertArrayEquals(packet.payload(), decoded.payload());
    }

    @Test
    void anyChangedHeaderByteIsCaught() {
        byte[] bytes = new Ipv4Packet(1, 2, 64, new byte[0]).encode();
        for (int i = 0; i < Ipv4Packet.HEADER; i++) {
            byte[] changed = bytes.clone();
            changed[i] ^= 0x01;
            assertThrows(BadPacketException.class, () -> Ipv4Packet.decode(changed), "byte changed");
        }
    }

    @Test
    void shortPacketsAreRefused() {
        assertThrows(BadPacketException.class, () -> Ipv4Packet.decode(new byte[5]));
        assertThrows(BadPacketException.class, () -> TcpSegment.decode(1, 2, new byte[5]));
    }

    @Test
    void tcpSegmentRoundTripsWithItsPayload() throws Exception {
        TcpSegment segment = new TcpSegment(51000, 443, 0xfffffff0L, 77, TcpSegment.PSH | TcpSegment.ACK, 1000, new byte[] {1, 2, 3});
        TcpSegment decoded = TcpSegment.decode(1, 2, segment.encode(1, 2));
        assertEquals(51000, decoded.sourcePort());
        assertEquals(443, decoded.destinationPort());
        assertEquals(0xfffffff0L, decoded.sequence());
        assertEquals(77, decoded.acknowledgement());
        assertTrue(decoded.has(TcpSegment.PSH) && decoded.has(TcpSegment.ACK));
        assertFalse(decoded.has(TcpSegment.SYN));
        assertArrayEquals(new byte[] {1, 2, 3}, decoded.payload());
    }

    @Test
    void tcpChecksumCoversTheAddresses() {
        byte[] bytes = new TcpSegment(1, 2, 3, 4, TcpSegment.SYN, 5, new byte[0]).encode(1, 2);
        assertThrows(BadPacketException.class, () -> TcpSegment.decode(1, 3, bytes));
    }

    @Test
    void anAttackerCanRecomputeTheChecksum() throws Exception {
        byte[] bytes = new Ipv4Packet(1, 2, 64, new byte[0]).encode();
        Ipv4Packet changed = new Ipv4Packet(1, 2, 1, Ipv4Packet.decode(bytes).payload());
        assertEquals(1, Ipv4Packet.decode(changed.encode()).timeToLive());
    }
}
