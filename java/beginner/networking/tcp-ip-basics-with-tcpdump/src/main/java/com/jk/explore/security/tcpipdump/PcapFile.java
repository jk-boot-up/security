package com.jk.explore.security.tcpipdump;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * The classic libpcap capture file format, which tcpdump and Wireshark read: a 24-byte file header, then for each packet a
 * 16-byte record header and the packet's bytes. The link type is 101 (raw IP), so each packet starts at its IPv4 header.
 */
final class PcapFile {

    static final int MAGIC = 0xa1b2c3d4;
    static final int LINKTYPE_RAW = 101;
    static final long FIRST_SECOND = 1_700_000_000L;

    private PcapFile() {
    }

    /** Writes the packets, one second apart from a fixed start, so the file is the same every time. */
    static byte[] write(List<byte[]> packets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(MAGIC).putShort((short) 2).putShort((short) 4).putInt(0).putInt(0).putInt(65535).putInt(LINKTYPE_RAW);
        out.writeBytes(header.array());
        long second = FIRST_SECOND;
        for (byte[] packet : packets) {
            ByteBuffer record = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            record.putInt((int) second++).putInt(0).putInt(packet.length).putInt(packet.length);
            out.writeBytes(record.array());
            out.writeBytes(packet);
        }
        return out.toByteArray();
    }

    static List<byte[]> read(byte[] file) throws BadPacketException {
        if (file.length < 24) {
            throw new BadPacketException("file shorter than a pcap header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt() != MAGIC) {
            throw new BadPacketException("not a little-endian pcap file");
        }
        buffer.position(20);
        if (buffer.getInt() != LINKTYPE_RAW) {
            throw new BadPacketException("not a raw IP capture");
        }
        List<byte[]> packets = new ArrayList<>();
        while (buffer.remaining() > 0) {
            if (buffer.remaining() < 16) {
                throw new BadPacketException("record header cut short");
            }
            buffer.position(buffer.position() + 8);
            int captured = buffer.getInt();
            buffer.getInt();
            if (captured < 0 || captured > buffer.remaining()) {
                throw new BadPacketException("packet cut short");
            }
            byte[] packet = new byte[captured];
            buffer.get(packet);
            packets.add(packet);
        }
        return packets;
    }
}
