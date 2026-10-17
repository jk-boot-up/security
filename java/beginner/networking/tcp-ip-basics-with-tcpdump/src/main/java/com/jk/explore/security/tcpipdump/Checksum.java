package com.jk.explore.security.tcpipdump;

/** The Internet checksum of RFC 1071: the ones' complement of the ones' complement sum of 16-bit words. */
final class Checksum {

    private Checksum() {
    }

    /** The sum of the 16-bit words, folded to 16 bits, before the final complement. */
    static int sum(byte[] data) {
        long total = 0;
        for (int i = 0; i < data.length; i += 2) {
            int high = data[i] & 0xff;
            int low = i + 1 < data.length ? data[i + 1] & 0xff : 0;
            total += (high << 8) | low;
        }
        while ((total >> 16) != 0) {
            total = (total & 0xffff) + (total >> 16);
        }
        return (int) total;
    }

    static int of(byte[] data) {
        return ~sum(data) & 0xffff;
    }
}
