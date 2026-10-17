package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 as lower-case hex: the fingerprint a manifest records for each file. */
final class Sha256 {

    private Sha256() {
    }

    static String of(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String of(Path file) throws IOException {
        return of(Files.readAllBytes(file));
    }
}
