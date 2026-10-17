package com.jk.explore.security.tcpipdump;

import java.nio.ByteBuffer;

/** A TCP segment with a 20-byte header (no options). Sequence and acknowledgement numbers are 32-bit, held in a long. */
record TcpSegment(int sourcePort, int destinationPort, long sequence, long acknowledgement, int flags, int window, byte[] payload) {

    static final int FIN = 0x01;
    static final int SYN = 0x02;
    static final int RST = 0x04;
    static final int PSH = 0x08;
    static final int ACK = 0x10;
    static final int HEADER = 20;

    boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** Encodes the segment, with the checksum computed over a pseudo-header holding the addresses. */
    byte[] encode(int sourceAddress, int destinationAddress) {
        byte[] bytes = body(0);
        int checksum = Checksum.of(withPseudoHeader(sourceAddress, destinationAddress, bytes));
        bytes[16] = (byte) (checksum >> 8);
        bytes[17] = (byte) checksum;
        return bytes;
    }

    private byte[] body(int checksum) {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER + payload.length);
        buffer.putShort((short) sourcePort).putShort((short) destinationPort);
        buffer.putInt((int) sequence).putInt((int) acknowledgement);
        buffer.put((byte) 0x50).put((byte) flags).putShort((short) window);
        buffer.putShort((short) checksum).putShort((short) 0);
        buffer.put(payload);
        return buffer.array();
    }

    static byte[] withPseudoHeader(int sourceAddress, int destinationAddress, byte[] segment) {
        ByteBuffer buffer = ByteBuffer.allocate(12 + segment.length);
        buffer.putInt(sourceAddress).putInt(destinationAddress).put((byte) 0).put((byte) 6).putShort((short) segment.length);
        buffer.put(segment);
        return buffer.array();
    }

    /** Reads the fields without checking the checksum, as a capture tool must: a damaged packet still has to be shown. */
    static TcpSegment parse(byte[] bytes) throws BadPacketException {
        if (bytes.length < HEADER) {
            throw new BadPacketException("segment shorter than a TCP header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int sourcePort = buffer.getShort() & 0xffff;
        int destinationPort = buffer.getShort() & 0xffff;
        long sequence = buffer.getInt() & 0xffffffffL;
        long acknowledgement = buffer.getInt() & 0xffffffffL;
        buffer.get();
        int flags = buffer.get() & 0xff;
        int window = buffer.getShort() & 0xffff;
        byte[] payload = new byte[bytes.length - HEADER];
        System.arraycopy(bytes, HEADER, payload, 0, payload.length);
        return new TcpSegment(sourcePort, destinationPort, sequence, acknowledgement, flags, window, payload);
    }

    static boolean checksumOk(int sourceAddress, int destinationAddress, byte[] bytes) {
        return Checksum.sum(withPseudoHeader(sourceAddress, destinationAddress, bytes)) == 0xffff;
    }

    static TcpSegment decode(int sourceAddress, int destinationAddress, byte[] bytes) throws BadPacketException {
        if (bytes.length >= HEADER && !checksumOk(sourceAddress, destinationAddress, bytes)) {
            throw new BadPacketException("TCP checksum does not match");
        }
        return parse(bytes);
    }
}
