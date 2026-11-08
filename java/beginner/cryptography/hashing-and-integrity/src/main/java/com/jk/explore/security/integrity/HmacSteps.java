package com.jk.explore.security.integrity;

/**
 * HMAC-SHA-256 worked out by hand, with nothing but SHA-256 and XOR, so every step can be shown. A test proves the result
 * is exactly the tag that Java's own {@code Mac} gives. For teaching only: real code uses {@link Hmac}.
 */
public final class HmacSteps {

    /** SHA-256 reads its input in blocks of 64 bytes, so HMAC pads the key to 64 bytes. */
    static final int BLOCK = 64;
    static final byte INNER_PAD = 0x36;
    static final byte OUTER_PAD = 0x5c;

    /** Every value on the way to the tag. */
    public record Steps(byte[] paddedKey, byte[] innerKey, byte[] innerHash, byte[] outerKey, byte[] tag) {
    }

    private HmacSteps() {
    }

    public static Steps of(byte[] key, byte[] message) {
        if (key.length > BLOCK) {
            key = Sha256.digest(key);                       // a long key is hashed first
        }
        byte[] padded = new byte[BLOCK];                    // the key, then zeros, to 64 bytes
        System.arraycopy(key, 0, padded, 0, key.length);
        // step 1: inner key; step 2: inner hash; step 3: outer key; step 4: the tag
        byte[] innerKey = xorEach(padded, INNER_PAD);
        byte[] innerHash = Sha256.digest(join(innerKey, message));
        byte[] outerKey = xorEach(padded, OUTER_PAD);
        byte[] tag = Sha256.digest(join(outerKey, innerHash));
        return new Steps(padded, innerKey, innerHash, outerKey, tag);
    }

    private static byte[] xorEach(byte[] bytes, byte pad) {
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = (byte) (bytes[i] ^ pad);
        }
        return out;
    }

    private static byte[] join(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
