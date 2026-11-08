package com.jk.explore.security.tcpip;

/** The Internet checksum of RFC 1071: the ones' complement of the ones' complement sum of 16-bit words. */
final class Checksum {

    private Checksum() {
    }

    /** Step 1: the data as 16-bit words (an odd last byte is padded with a zero). */
    static int[] words(byte[] data) {
        int[] words = new int[(data.length + 1) / 2];
        for (int i = 0; i < data.length; i += 2) {
            int high = data[i] & 0xff;
            int low = i + 1 < data.length ? data[i + 1] & 0xff : 0;
            words[i / 2] = (high << 8) | low;
        }
        return words;
    }

    /** Step 2: the plain sum of the words, which may be longer than 16 bits. */
    static long total(byte[] data) {
        long total = 0;
        for (int word : words(data)) {
            total += word;
        }
        return total;
    }

    /** Step 3: the sum folded to 16 bits, by adding what overflowed back in at the bottom. */
    static int sum(byte[] data) {
        long total = total(data);
        while ((total >> 16) != 0) {
            total = (total & 0xffff) + (total >> 16);
        }
        return (int) total;
    }

    /** Step 4: every bit flipped (ffff minus the folded sum): the checksum written into the header. */
    static int of(byte[] data) {
        return ~sum(data) & 0xffff;
    }
}
