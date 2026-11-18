package com.jk.explore.security.tlscapture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

/**
 * The first record a client sends, read by hand from the bytes: what anyone on the path can read before any key exists. tshark reads the same fields; this class
 * shows where they are.
 */
record ClientHello(int recordType, int messageType, String random, String serverName) {

    static final int HANDSHAKE = 22;
    static final int CLIENT_HELLO = 1;
    private static final int SERVER_NAME = 0;

    /** Parses one TLS record that holds a Client Hello. */
    static ClientHello parse(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        int recordType = b.get() & 0xff;
        b.getShort(); // record version
        b.getShort(); // record length
        int messageType = b.get() & 0xff;
        b.position(b.position() + 3 + 2); // message length, legacy version
        byte[] random = new byte[32];
        b.get(random);
        skip(b, b.get() & 0xff); // session id
        skip(b, b.getShort() & 0xffff); // cipher suites
        skip(b, b.get() & 0xff); // compression methods
        int end = (b.getShort() & 0xffff) + b.position();
        String name = "";
        while (b.position() < end) {
            int type = b.getShort() & 0xffff;
            int length = b.getShort() & 0xffff;
            if (type == SERVER_NAME) {
                b.position(b.position() + 2 + 1); // list length, name type (0 = host name)
                byte[] host = new byte[b.getShort() & 0xffff];
                b.get(host);
                name = new String(host, StandardCharsets.US_ASCII);
            } else {
                skip(b, length);
            }
        }
        return new ClientHello(recordType, messageType, HexFormat.of().formatHex(random), name);
    }

    private static void skip(ByteBuffer b, int n) {
        b.position(b.position() + n);
    }

    /** Reads the Client Hello from a capture file: the client's bytes, put back together, up to the end of the first record. */
    static ClientHello read(Path capture) throws IOException {
        byte[] sent = clientBytes(capture);
        int length = 5 + (((sent[3] & 0xff) << 8) | (sent[4] & 0xff));
        return parse(java.util.Arrays.copyOf(sent, length));
    }

    /** Every byte the client sent in a capture that {@link Pcap} wrote: the first packet is the client's SYN, so its source port is the client's. */
    static byte[] clientBytes(Path capture) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(capture)).order(ByteOrder.LITTLE_ENDIAN);
        b.position(24);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        int clientPort = -1;
        while (b.remaining() >= 16) {
            b.position(b.position() + 8);
            int length = b.getInt();
            b.getInt();
            byte[] ip = new byte[length];
            b.get(ip);
            int sourcePort = ((ip[20] & 0xff) << 8) | (ip[21] & 0xff);
            if (clientPort < 0) {
                clientPort = sourcePort;
            }
            if (sourcePort == clientPort) {
                sent.write(ip, 40, length - 40);
            }
        }
        return sent.toByteArray();
    }
}
