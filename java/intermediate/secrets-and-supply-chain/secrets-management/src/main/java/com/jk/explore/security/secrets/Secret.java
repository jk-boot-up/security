package com.jk.explore.security.secrets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * A secret value that cannot be printed by accident: toString, string concatenation and log lines show "[redacted]".
 * It is held in a char array that can be wiped once the secret is no longer needed.
 */
final class Secret {

    private final char[] value;
    private boolean wiped;

    private Secret(char[] value) {
        this.value = value;
    }

    /** Takes ownership of the array: the caller must not keep or reuse it. */
    static Secret of(char[] value) {
        return new Secret(value);
    }

    static Secret of(String value) {
        return new Secret(value.toCharArray());
    }

    /** Lets a caller use the value without it ever becoming a String. */
    <T> T use(java.util.function.Function<char[], T> user) {
        if (wiped) {
            throw new IllegalStateException("the secret has been wiped");
        }
        return user.apply(value);
    }

    /** Constant-time comparison, so the time a check takes does not tell how many characters matched. */
    boolean matches(char[] candidate) {
        if (wiped) {
            return false;
        }
        return MessageDigest.isEqual(bytes(value), bytes(candidate));
    }

    boolean matches(Secret other) {
        return other.use(this::matches);
    }

    void wipe() {
        Arrays.fill(value, '\0');
        wiped = true;
    }

    boolean isWiped() {
        return wiped;
    }

    int length() {
        return value.length;
    }

    @Override
    public String toString() {
        return "[redacted]";
    }

    private static byte[] bytes(char[] chars) {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] out = new byte[buffer.remaining()];
        buffer.get(out);
        return out;
    }
}
