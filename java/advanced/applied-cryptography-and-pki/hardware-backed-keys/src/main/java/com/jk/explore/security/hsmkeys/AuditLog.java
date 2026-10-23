package com.jk.explore.security.hsmkeys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** A hash-chained record of what was signed: each entry carries the hash of the one before, so an edit or a removed entry breaks the chain. */
final class AuditLog {

    record Entry(int seq, String event, String file, String contentHash, String previous, String hash) {
    }

    private final List<Entry> entries = new ArrayList<>();

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String chain(int seq, String event, String file, String contentHash, String previous) {
        return sha256((seq + "|" + event + "|" + file + "|" + contentHash + "|" + previous).getBytes(StandardCharsets.UTF_8));
    }

    void append(String event, String file, byte[] content) {
        String previous = entries.isEmpty() ? "genesis" : entries.getLast().hash();
        String contentHash = sha256(content);
        entries.add(new Entry(entries.size() + 1, event, file, contentHash, previous, chain(entries.size() + 1, event, file, contentHash, previous)));
    }

    List<Entry> entries() {
        return List.copyOf(entries);
    }

    int count(String event) {
        return (int) entries.stream().filter(e -> e.event().equals(event)).count();
    }

    /** True when every entry still matches its hash and its predecessor. */
    boolean verify() {
        return verify(entries);
    }

    static boolean verify(List<Entry> list) {
        String previous = "genesis";
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            if (e.seq() != i + 1 || !e.previous().equals(previous) || !e.hash().equals(chain(e.seq(), e.event(), e.file(), e.contentHash(), e.previous()))) {
                return false;
            }
            previous = e.hash();
        }
        return true;
    }
}
