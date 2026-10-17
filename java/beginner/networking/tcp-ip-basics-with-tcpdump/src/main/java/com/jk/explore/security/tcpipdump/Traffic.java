package com.jk.explore.security.tcpipdump;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The traffic the demo records: one real customer's purchase, then a SYN flood, then one damaged packet. All fixed, so every capture is identical. */
final class Traffic {

    static final int SHOP = Address.parse("203.0.113.10");
    static final int CUSTOMER = Address.parse("198.51.100.7");
    static final String ORDER = "card=4111111111111111&order=A-1001";
    static final int FLOOD_SOURCES = 8;

    private Traffic() {
    }

    private static byte[] packet(int from, int to, int fromPort, int toPort, long sequence, long acknowledgement, int flags, byte[] payload) {
        TcpSegment segment = new TcpSegment(fromPort, toPort, sequence, acknowledgement, flags, 65535, payload);
        return new Ipv4Packet(from, to, 64, segment.encode(from, to)).encode();
    }

    static List<byte[]> purchase() {
        byte[] order = ORDER.getBytes(StandardCharsets.UTF_8);
        List<byte[]> packets = new ArrayList<>();
        packets.add(packet(CUSTOMER, SHOP, 51000, 443, 1000, 0, TcpSegment.SYN, new byte[0]));
        packets.add(packet(SHOP, CUSTOMER, 443, 51000, 7000, 1001, TcpSegment.SYN | TcpSegment.ACK, new byte[0]));
        packets.add(packet(CUSTOMER, SHOP, 51000, 443, 1001, 7001, TcpSegment.ACK, new byte[0]));
        packets.add(packet(CUSTOMER, SHOP, 51000, 443, 1001, 7001, TcpSegment.PSH | TcpSegment.ACK, order));
        packets.add(packet(SHOP, CUSTOMER, 443, 51000, 7001, 1001 + order.length, TcpSegment.ACK, new byte[0]));
        return packets;
    }

    static List<byte[]> flood() {
        List<byte[]> packets = new ArrayList<>();
        for (int i = 1; i <= FLOOD_SOURCES; i++) {
            packets.add(packet(Address.parse("100.64.0." + i), SHOP, 40000 + i, 443, 7, 0, TcpSegment.SYN, new byte[0]));
        }
        return packets;
    }

    /** A SYN whose TCP checksum is wrong, as a damaged or carelessly forged packet would be. */
    static byte[] damaged() {
        int from = Address.parse("100.64.0.99");
        byte[] segment = new TcpSegment(40099, 443, 7, 0, TcpSegment.SYN, 65535, new byte[0]).encode(from, SHOP);
        segment[16] ^= 0x55;
        return new Ipv4Packet(from, SHOP, 64, segment).encode();
    }

    static List<byte[]> everything() {
        List<byte[]> packets = new ArrayList<>(purchase());
        packets.addAll(flood());
        packets.add(damaged());
        return packets;
    }
}
