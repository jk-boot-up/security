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

    /** Reads a CBOR map of small integer keys to small integers or byte strings, as in a COSE key, into its keys and values in order. */
    static java.util.List<Object[]> readMap(byte[] bytes) {
        java.util.List<Object[]> out = new java.util.ArrayList<>();
        int[] at = {0};
        int head = bytes[at[0]++] & 0xff;
        if (head >> 5 != 5) {
            throw new IllegalArgumentException("not a CBOR map");
        }
        for (int i = 0; i < (head & 0x1f); i++) {
            out.add(new Object[] {read(bytes, at), read(bytes, at)});
        }
        return out;
    }

    private static Object read(byte[] bytes, int[] at) {
        int head = bytes[at[0]++] & 0xff;
        int type = head >> 5;
        int value = head & 0x1f;
        if (value == 24) {
            value = bytes[at[0]++] & 0xff;
        } else if (value > 24) {
            throw new IllegalArgumentException("only short CBOR items are read here");
        }
        return switch (type) {
            case 0 -> value;
            case 1 -> -1 - value;
            case 2 -> {
                byte[] b = java.util.Arrays.copyOfRange(bytes, at[0], at[0] + value);
                at[0] += value;
                yield b;
            }
            default -> throw new IllegalArgumentException("only integers and byte strings are read here");
        };
    }
}
