package com.jk.explore.security.tls;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** What a sniffer sees: the bytes that cross the network, and the TLS record headers in them. */
final class Wire {

    static final int CHANGE_CIPHER_SPEC = 20;
    static final int ALERT = 21;
    static final int HANDSHAKE = 22;
    static final int APPLICATION_DATA = 23;

    private final ByteArrayOutputStream fromClient = new ByteArrayOutputStream();
    private final ByteArrayOutputStream fromServer = new ByteArrayOutputStream();

    void clientSent(byte[] bytes) {
        fromClient.writeBytes(bytes);
    }

    void serverSent(byte[] bytes) {
        fromServer.writeBytes(bytes);
    }

    byte[] clientBytes() {
        return fromClient.toByteArray();
    }

    byte[] serverBytes() {
        return fromServer.toByteArray();
    }

    /** The type byte of every TLS record in the bytes, in order: 22 handshake, 20 change cipher spec, 21 alert, 23 application data. */
    static List<Integer> recordTypes(byte[] bytes) {
        List<Integer> types = new ArrayList<>();
        int at = 0;
        while (at + 5 <= bytes.length) {
            types.add(bytes[at] & 0xff);
            int length = ((bytes[at + 3] & 0xff) << 8) | (bytes[at + 4] & 0xff);
            at += 5 + length;
        }
        return types;
    }

    static boolean contains(byte[] bytes, String text) {
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(text);
    }
}
