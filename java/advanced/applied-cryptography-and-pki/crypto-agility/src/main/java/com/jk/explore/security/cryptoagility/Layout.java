package com.jk.explore.security.cryptoagility;

import java.util.HexFormat;

/**
 * A teaching class: splits a sealed envelope into its parts (header, IV or nonce, ciphertext, tag) so the video can show where every byte goes. The
 * lengths follow from the suite in the header; a test proves they add up to the real envelope for every suite.
 */
public record Layout(String header, int iv, int ciphertext, int tag, int total) {

    public static final int HEADER = 3;

    public static Layout of(byte[] blob) {
        int version = Envelope.versionOf(blob);
        int iv = version == Envelope.V1_CBC_HMAC ? 16 : 12;
        int tag = version == Envelope.V1_CBC_HMAC ? 32 : 16;
        String header = HexFormat.ofDelimiter(" ").formatHex(blob, 0, HEADER);
        return new Layout(header, iv, blob.length - HEADER - iv - tag, tag, blob.length);
    }

    /** CBC pads the text to whole 16-byte blocks, and always adds at least one byte of padding. */
    public static int cbcLength(int plainBytes) {
        return (plainBytes / 16 + 1) * 16;
    }
}
