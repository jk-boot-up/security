package com.jk.explore.security.provenance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a jar-like zip archive. A jar records each entry's time and the order the entries were added, so two builds of the same
 * source differ if the clock or the file order differ. The reproducible build fixes both: one agreed time for every entry, and entries
 * sorted by name. Entries are stored, not compressed, so the bytes do not depend on the compression library either.
 */
final class Archive {

    /** The fixed time every entry gets in a reproducible build, like SOURCE_DATE_EPOCH: the time of the source commit. */
    static final LocalDateTime COMMIT_TIME = LocalDateTime.of(2026, 10, 1, 9, 0, 0);

    private Archive() {
    }

    /** A careless build: entries in the order given (as a file system lists them) and stamped with the build's own clock. */
    static byte[] careless(List<String> listingOrder, Map<String, byte[]> files, LocalDateTime buildClock) {
        return zip(listingOrder, files, buildClock);
    }

    /** A reproducible build: entries sorted by name and stamped with the commit time. */
    static byte[] reproducible(Map<String, byte[]> files) {
        return zip(new ArrayList<>(new TreeMap<>(files).keySet()), files, COMMIT_TIME);
    }

    private static byte[] zip(List<String> order, Map<String, byte[]> files, LocalDateTime time) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : order) {
                byte[] content = files.get(name);
                ZipEntry entry = new ZipEntry(name);
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(content.length);
                entry.setCompressedSize(content.length);
                CRC32 crc = new CRC32();
                crc.update(content);
                entry.setCrc(crc.getValue());
                entry.setTimeLocal(time);
                zip.putNextEntry(entry);
                zip.write(content);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
