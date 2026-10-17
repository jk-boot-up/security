package com.jk.explore.security.certs;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** The small part of DER (the binary encoding behind X.509) that a certificate needs: tag, length, value. */
final class Der {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private Der() {
    }

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            body.writeBytes(part);
        }
        byte[] value = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (value.length < 128) {
            out.write(value.length);
        } else if (value.length < 256) {
            out.write(0x81);
            out.write(value.length);
        } else {
            out.write(0x82);
            out.write(value.length >> 8);
            out.write(value.length);
        }
        out.writeBytes(value);
        return out.toByteArray();
    }

    static byte[] sequence(byte[]... parts) {
        return tlv(0x30, parts);
    }

    static byte[] set(byte[]... parts) {
        return tlv(0x31, parts);
    }

    static byte[] integer(long value) {
        return tlv(0x02, BigInteger.valueOf(value).toByteArray());
    }

    static byte[] bool(boolean value) {
        return tlv(0x01, new byte[] {(byte) (value ? 0xff : 0x00)});
    }

    static byte[] utf8(String text) {
        return tlv(0x0c, text.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] ia5(int tag, String text) {
        return tlv(tag, text.getBytes(StandardCharsets.US_ASCII));
    }

    static byte[] utcTime(LocalDateTime time) {
        return ia5(0x17, UTC_TIME.format(time) + "Z");
    }

    static byte[] octets(byte[] value) {
        return tlv(0x04, value);
    }

    static byte[] bitString(int unusedBits, byte... value) {
        byte[] content = new byte[value.length + 1];
        content[0] = (byte) unusedBits;
        System.arraycopy(value, 0, content, 1, value.length);
        return tlv(0x03, content);
    }

    /** [n] EXPLICIT: the value wrapped in a context-specific constructed tag. */
    static byte[] explicit(int number, byte[] inner) {
        return tlv(0xa0 + number, inner);
    }

    static byte[] oid(String dotted) {
        String[] parts = dotted.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            long value = Long.parseLong(parts[i]);
            int groups = 1;
            while ((value >> (7 * groups)) != 0) {
                groups++;
            }
            for (int g = groups - 1; g >= 0; g--) {
                int bits = (int) ((value >> (7 * g)) & 0x7f);
                out.write(g > 0 ? bits | 0x80 : bits);
            }
        }
        return tlv(0x06, out.toByteArray());
    }
}
