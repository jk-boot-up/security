package com.jk.explore.security.tcpip;

import java.nio.ByteBuffer;

/** An IPv4 packet with a 20-byte header (no options) carrying a TCP segment. */
record Ipv4Packet(int source, int destination, int timeToLive, byte[] payload) {

    static final int HEADER = 20;
    static final int TCP = 6;

    byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER + payload.length);
        buffer.put((byte) 0x45).put((byte) 0).putShort((short) (HEADER + payload.length));
        buffer.putShort((short) 0).putShort((short) 0);
        buffer.put((byte) timeToLive).put((byte) TCP).putShort((short) 0);
        buffer.putInt(source).putInt(destination);
        buffer.put(payload);
        byte[] bytes = buffer.array();
        int checksum = Checksum.of(java.util.Arrays.copyOf(bytes, HEADER));
        bytes[10] = (byte) (checksum >> 8);
        bytes[11] = (byte) checksum;
        return bytes;
    }

    static Ipv4Packet decode(byte[] bytes) throws BadPacketException {
        if (bytes.length < HEADER) {
            throw new BadPacketException("packet shorter than an IPv4 header");
        }
        if (bytes[0] != 0x45) {
            throw new BadPacketException("not an IPv4 header without options");
        }
        if (Checksum.sum(java.util.Arrays.copyOf(bytes, HEADER)) != 0xffff) {
            throw new BadPacketException("IPv4 header checksum does not match");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(8);
        int timeToLive = buffer.get() & 0xff;
        buffer.position(12);
        int source = buffer.getInt();
        int destination = buffer.getInt();
        byte[] payload = new byte[bytes.length - HEADER];
        System.arraycopy(bytes, HEADER, payload, 0, payload.length);
        return new Ipv4Packet(source, destination, timeToLive, payload);
    }
}
