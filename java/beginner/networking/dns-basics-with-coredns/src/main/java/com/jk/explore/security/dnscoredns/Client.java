package com.jk.explore.security.dnscoredns;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/** Asks a DNS server a question over UDP (one datagram each way) or over TCP (each message starts with its length in two bytes). */
final class Client {

    /** A decoded reply with the size of the message that carried it. */
    record Reply(DnsMessage message, int bytes) {
    }

    static final int TIMEOUT_MILLIS = 5000;
    private static final int ID_FOR_PROBES = 0x5151;

    private Client() {
    }

    static Reply udp(int port, DnsMessage query) throws IOException, BadMessageException {
        InetAddress local = InetAddress.getLoopbackAddress();
        byte[] out = query.encode();
        try (DatagramSocket socket = new DatagramSocket(0, local)) {
            socket.setSoTimeout(TIMEOUT_MILLIS);
            socket.send(new DatagramPacket(out, out.length, local, port));
            DatagramPacket in = new DatagramPacket(new byte[4096], 4096);
            socket.receive(in);
            byte[] bytes = java.util.Arrays.copyOf(in.getData(), in.getLength());
            return new Reply(DnsMessage.decode(bytes), bytes.length);
        }
    }

    /** The reply's size includes the two length bytes that TCP adds. */
    static Reply tcp(int port, DnsMessage query) throws IOException, BadMessageException {
        byte[] out = query.encode();
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(TIMEOUT_MILLIS);
            ByteArrayOutputStream framed = new ByteArrayOutputStream();
            framed.write(out.length >> 8);
            framed.write(out.length & 0xff);
            framed.writeBytes(out);
            socket.getOutputStream().write(framed.toByteArray());
            socket.getOutputStream().flush();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int length = in.readUnsignedShort();
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            return new Reply(DnsMessage.decode(bytes), length + 2);
        }
    }

    static boolean tcpAnswers(int port) {
        try {
            return tcp(port, DnsMessage.query(ID_FOR_PROBES, "www." + ShopZone.ZONE)).message().id() == ID_FOR_PROBES;
        } catch (IOException | BadMessageException e) {
            return false;
        }
    }
}
