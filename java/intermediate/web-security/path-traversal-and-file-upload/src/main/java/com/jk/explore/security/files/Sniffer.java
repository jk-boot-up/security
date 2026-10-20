package com.jk.explore.security.files;

/** Decides what a file is from its first bytes, the only part the customer cannot rename. */
public final class Sniffer {

    public record Kind(String extension, String contentType) {
    }

    private Sniffer() {
    }

    public static Kind of(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return new Kind("png", "image/png");
        }
        if (b.length >= 4 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff) {
            return new Kind("jpg", "image/jpeg");
        }
        return null;
    }
}
