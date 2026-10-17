package com.jk.explore.security.integrity;

import java.util.zip.CRC32;

/**
 * A cyclic redundancy check: a short number that changes when data is damaged by accident, such as a flipped bit on a
 * disk or a noisy line. It is fast and it is not secure: anyone can work out a new CRC for changed data.
 */
public final class Crc32Checksum {

    private Crc32Checksum() {
    }

    public static String of(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return String.format("%08x", crc.getValue());
    }
}
