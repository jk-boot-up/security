package com.jk.explore.security.ransomres;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 as lower-case hex: the checksum that the manifests carry. */
final class Sha {

    private Sha() {
    }

    static String hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(String text) {
        return hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
