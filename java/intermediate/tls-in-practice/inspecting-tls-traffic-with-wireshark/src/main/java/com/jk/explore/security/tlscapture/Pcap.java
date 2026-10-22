package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a capture file (pcap, raw IPv4 packets) of one TCP connection between two loopback ports, from the bytes a tap saw. */
final class Pcap implements AutoCloseable {

    private static final int SYN = 0x02;
    private static final int ACK = 0x10;
    private static final int PSH = 0x08;
    private static final int FIN = 0x01;
    private static final int MSS = 1400;

    private final OutputStream out;
    private final int clientPort;
    private final int serverPort;
    private long clientSeq = 1000;
    private long serverSeq = 5000;

    Pcap(Path file, int clientPort, int serverPort) throws IOException {
        this.out = Files.newOutputStream(file);
        this.clientPort = clientPort;
        this.serverPort = serverPort;
        ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0xa1b2c3d4).putShort((short) 2).putShort((short) 4).putInt(0).putInt(0).putInt(65535).putInt(101);
        out.write(header.array());
        packet(true, SYN, new byte[0]);
        clientSeq++;
        packet(false, SYN | ACK, new byte[0]);
        serverSeq++;
        packet(true, ACK, new byte[0]);
    }

    /** Bytes that the client sent, or that the server sent. */
    synchronized void data(boolean fromClient, byte[] bytes, int length) throws IOException {
        for (int at = 0; at < length; at += MSS) {
            byte[] chunk = java.util.Arrays.copyOfRange(bytes, at, Math.min(length, at + MSS));
            packet(fromClient, PSH | ACK, chunk);
            if (fromClient) {
                clientSeq += chunk.length;
            } else {
                serverSeq += chunk.length;
            }
        }
    }

    private void packet(boolean fromClient, int flags, byte[] payload) throws IOException {
        int sport = fromClient ? clientPort : serverPort;
        int dport = fromClient ? serverPort : clientPort;
        long seq = fromClient ? clientSeq : serverSeq;
        long ack = flags == SYN ? 0 : fromClient ? serverSeq : clientSeq;
        byte[] tcp = new byte[20 + payload.length];
        ByteBuffer t = ByteBuffer.wrap(tcp);
        t.putShort((short) sport).putShort((short) dport).putInt((int) seq).putInt((int) ack).put((byte) 0x50).put((byte) flags).putShort((short) 65535).putShort((short) 0).putShort((short) 0).put(payload);
        int checksum = checksum(pseudo(tcp.length), tcp);
        tcp[16] = (byte) (checksum >> 8);
        tcp[17] = (byte) checksum;
        byte[] ip = new byte[20 + tcp.length];
        ByteBuffer i = ByteBuffer.wrap(ip);
        i.put((byte) 0x45).put((byte) 0).putShort((short) ip.length).putShort((short) 0).putShort((short) 0x4000).put((byte) 64).put((byte) 6).putShort((short) 0)
                .put(new byte[] {127, 0, 0, 1}).put(new byte[] {127, 0, 0, 1}).put(tcp);
        int ipChecksum = checksum(new byte[0], java.util.Arrays.copyOf(ip, 20));
        ip[10] = (byte) (ipChecksum >> 8);
        ip[11] = (byte) ipChecksum;
        long now = System.currentTimeMillis();
        ByteBuffer record = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        record.putInt((int) (now / 1000)).putInt((int) (now % 1000 * 1000)).putInt(ip.length).putInt(ip.length);
        out.write(record.array());
        out.write(ip);
    }

    private static byte[] pseudo(int tcpLength) {
        return new byte[] {127, 0, 0, 1, 127, 0, 0, 1, 0, 6, (byte) (tcpLength >> 8), (byte) tcpLength};
    }

    private static int checksum(byte[] prefix, byte[] data) {
        long sum = 0;
        byte[] all = new byte[prefix.length + data.length];
        System.arraycopy(prefix, 0, all, 0, prefix.length);
        System.arraycopy(data, 0, all, prefix.length, data.length);
        for (int k = 0; k + 1 < all.length; k += 2) {
            sum += ((all[k] & 0xff) << 8) | (all[k + 1] & 0xff);
        }
        if (all.length % 2 == 1) {
            sum += (all[all.length - 1] & 0xff) << 8;
        }
        while (sum >> 16 != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        return (int) (~sum & 0xffff);
    }

    @Override
    public synchronized void close() throws IOException {
        packet(true, FIN | ACK, new byte[0]);
        clientSeq++;
        packet(false, FIN | ACK, new byte[0]);
        serverSeq++;
        packet(true, ACK, new byte[0]);
        out.close();
    }
}
