package com.jk.explore.security.keystore;

/**
 * The first and worst way to keep a key: write it in the source. It is then in the compiled file, in every copy of the program, in the
 * build server, and in the history of the source repository. The value is a demo; nothing real may ever look like this.
 */
public final class HardCodedKey {

    public static final String KEY_TEXT = "hard-coded-statement-key-32by!";

    private HardCodedKey() {
    }

    public static byte[] key() {
        return Hex.bytes(KEY_TEXT);
    }
}
