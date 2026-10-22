package com.jk.explore.security.passkeysspring;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** The few CBOR items that an attestation object and a COSE public key need: integers, byte strings, text strings and maps. */
final class Cbor {

    private Cbor() {
    }

    static byte[] map(Object... keyValues) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        head(out, 5, keyValues.length / 2);
        for (Object item : keyValues) {
            item(out, item);
        }
        return out.toByteArray();
    }

    private static void item(ByteArrayOutputStream out, Object value) {
        switch (value) {
            case Integer i -> {
                if (i >= 0) {
                    head(out, 0, i);
                } else {
                    head(out, 1, -1 - i);
                }
            }
            case byte[] b -> {
                head(out, 2, b.length);
                out.writeBytes(b);
            }
            case String s -> {
                byte[] b = s.getBytes(StandardCharsets.UTF_8);
                head(out, 3, b.length);
                out.writeBytes(b);
            }
            case Map<?, ?> m when m.isEmpty() -> head(out, 5, 0);
            default -> throw new IllegalArgumentException("not supported: " + value);
        }
    }

    private static void head(ByteArrayOutputStream out, int type, int value) {
        if (value < 24) {
            out.write((type << 5) | value);
        } else if (value < 256) {
            out.write((type << 5) | 24);
            out.write(value);
        } else {
            out.write((type << 5) | 25);
            out.write(value >> 8);
            out.write(value & 0xff);
        }
    }
}
