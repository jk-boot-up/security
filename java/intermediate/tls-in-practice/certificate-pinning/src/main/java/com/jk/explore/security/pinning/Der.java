package com.jk.explore.security.pinning;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** The few ASN.1 DER encodings an X.509 certificate and a revocation list need. Every value is a tag, a length and the bytes. */
final class Der {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    private Der() {
    }

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            body.writeBytes(p);
        }
        byte[] content = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (content.length < 128) {
            out.write(content.length);
        } else {
            byte[] len = BigInteger.valueOf(content.length).toByteArray();
            int skip = len[0] == 0 ? 1 : 0;
            out.write(0x80 | (len.length - skip));
            out.write(len, skip, len.length - skip);
        }
        out.writeBytes(content);
        return out.toByteArray();
    }

    static byte[] sequence(byte[]... parts) {
        return tlv(0x30, parts);
    }

    static byte[] set(byte[]... parts) {
        return tlv(0x31, parts);
    }

    static byte[] integer(BigInteger n) {
        return tlv(0x02, n.toByteArray());
    }

    static byte[] integer(long n) {
        return integer(BigInteger.valueOf(n));
    }

    static byte[] bool(boolean b) {
        return tlv(0x01, new byte[] {(byte) (b ? 0xff : 0)});
    }

    static byte[] octets(byte[] b) {
        return tlv(0x04, b);
    }

    static byte[] utf8(String s) {
        return tlv(0x0c, s.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] time(Instant t) {
        return tlv(0x17, UTC_TIME.format(t).getBytes(StandardCharsets.US_ASCII));
    }

    /** A bit string with the given number of unused bits in its last byte. */
    static byte[] bits(int unused, byte... data) {
        byte[] content = new byte[data.length + 1];
        content[0] = (byte) unused;
        System.arraycopy(data, 0, content, 1, data.length);
        return tlv(0x03, content);
    }

    static byte[] oid(String dotted) {
        String[] parts = dotted.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            long v = Long.parseLong(parts[i]);
            byte[] tmp = new byte[10];
            int n = 0;
            do {
                tmp[n++] = (byte) (v & 0x7f);
                v >>= 7;
            } while (v > 0);
            for (int k = n - 1; k >= 0; k--) {
                out.write(tmp[k] | (k > 0 ? 0x80 : 0));
            }
        }
        return tlv(0x06, out.toByteArray());
    }

    /** [n] EXPLICIT: the wrapped value inside a context-specific constructed tag. */
    static byte[] explicit(int n, byte[] inner) {
        return tlv(0xa0 | n, inner);
    }

    /** A context-specific primitive string, as used for the names in the subject alternative name extension. */
    static byte[] contextString(int n, String s) {
        return tlv(0x80 | n, s.getBytes(StandardCharsets.US_ASCII));
    }

    /** A distinguished name with a single common name. */
    static byte[] commonName(String cn) {
        return sequence(set(sequence(oid("2.5.4.3"), utf8(cn))));
    }
}
