package com.jk.explore.security.tlsattacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads the bytes of a recorded TLS connection: records, handshake messages and the few fields this lab needs. */
final class TlsRecords {

    static final int CHANGE_CIPHER_SPEC = 20;
    static final int HANDSHAKE = 22;
    static final int APPLICATION_DATA = 23;
    static final int CLIENT_HELLO = 1;
    static final int SERVER_HELLO = 2;
    static final int CLIENT_KEY_EXCHANGE = 16;

    /** One TLS record: type, the version on the wire and the body. */
    record Rec(int type, int version, byte[] body) {
    }

    private TlsRecords() {
    }

    /** Splits a byte stream into whole records; a record cut off at the end is ignored. */
    static List<Rec> records(byte[] stream) {
        List<Rec> out = new ArrayList<>();
        int i = 0;
        while (i + 5 <= stream.length) {
            int len = u16(stream, i + 3);
            if (i + 5 + len > stream.length) {
                break;
            }
            out.add(new Rec(stream[i] & 0xff, u16(stream, i + 1), Arrays.copyOfRange(stream, i + 5, i + 5 + len)));
            i += 5 + len;
        }
        return out;
    }

    /** The handshake messages sent in the clear: every handshake record before the first change cipher spec, joined. */
    static List<byte[]> clearHandshake(List<Rec> records) {
        java.io.ByteArrayOutputStream joined = new java.io.ByteArrayOutputStream();
        for (Rec r : records) {
            if (r.type() == CHANGE_CIPHER_SPEC) {
                break;
            }
            if (r.type() == HANDSHAKE) {
                joined.writeBytes(r.body());
            }
        }
        byte[] all = joined.toByteArray();
        List<byte[]> messages = new ArrayList<>();
        int i = 0;
        while (i + 4 <= all.length) {
            int len = (all[i + 1] & 0xff) << 16 | u16(all, i + 2);
            if (i + 4 + len > all.length) {
                break;
            }
            messages.add(Arrays.copyOfRange(all, i, i + 4 + len));
            i += 4 + len;
        }
        return messages;
    }

    static byte[] message(List<byte[]> messages, int type) {
        for (byte[] m : messages) {
            if ((m[0] & 0xff) == type) {
                return m;
            }
        }
        return null;
    }

    /** The 32 random bytes of a ClientHello or ServerHello message. */
    static byte[] random(byte[] hello) {
        return Arrays.copyOfRange(hello, 6, 38);
    }

    /** The version a ClientHello states (its legacy version field: 0x0303 even for TLS 1.3). */
    static int clientHelloVersion(byte[] firstBytes) {
        if (firstBytes.length < 11 || firstBytes[0] != HANDSHAKE || firstBytes[5] != CLIENT_HELLO) {
            return -1;
        }
        return u16(firstBytes, 9);
    }

    /** The compression methods a ClientHello offers. */
    static List<Integer> compressionMethods(byte[] clientHello) {
        int i = 38;
        i += 1 + (clientHello[i] & 0xff);
        i += 2 + u16(clientHello, i);
        int n = clientHello[i] & 0xff;
        List<Integer> methods = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            methods.add(clientHello[i + 1 + k] & 0xff);
        }
        return methods;
    }

    /** The cipher suite a ServerHello chose. */
    static int chosenSuite(byte[] serverHello) {
        int i = 38;
        i += 1 + (serverHello[i] & 0xff);
        return u16(serverHello, i);
    }

    /** The last eight random bytes of a ServerHello: "DOWNGRD" and 1 (TLS 1.2) or 0 (TLS 1.1 and below) when a TLS 1.3 server was pushed down. */
    static String downgradeSentinel(byte[] serverHello) {
        byte[] r = random(serverHello);
        byte[] tail = Arrays.copyOfRange(r, 24, 32);
        byte[] marker = {0x44, 0x4F, 0x57, 0x4E, 0x47, 0x52, 0x44};
        if (Arrays.equals(Arrays.copyOfRange(tail, 0, 7), marker) && tail[7] <= 1) {
            return tail[7] == 1 ? "TLS 1.2" : "TLS 1.1 or lower";
        }
        return "none";
    }

    static int u16(byte[] b, int i) {
        return (b[i] & 0xff) << 8 | (b[i + 1] & 0xff);
    }
}
