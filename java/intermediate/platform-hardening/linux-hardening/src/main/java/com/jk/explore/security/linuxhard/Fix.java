package com.jk.explore.security.linuxhard;

/** One permission change: the mode before, the bits taken away or kept, and the mode after. */
public record Fix(String path, int before, String operator, int bits, int after, String why) {

    /** The change worked out, for example {@code 4755 - 4000 = 0755}. */
    public String steps() {
        return Modes.octal(before) + " " + operator + " " + Modes.octal(bits) + " = " + Modes.octal(after);
    }
}
