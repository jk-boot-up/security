package com.jk.explore.security.tlslog;

import java.util.HexFormat;

/** Lower-case hexadecimal, the way key log files and packet tools print bytes. */
final class Hex {

    private Hex() {
    }

    static String of(byte[] b) {
        return HexFormat.of().formatHex(b);
    }

    static byte[] parse(String s) {
        return HexFormat.of().parseHex(s);
    }
}
