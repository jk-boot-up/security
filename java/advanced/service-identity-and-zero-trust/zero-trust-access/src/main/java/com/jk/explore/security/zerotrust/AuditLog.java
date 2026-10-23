package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One JSON line per decision: who, what, allow or deny, and why. It is given only fields, never a token or a header, so a token cannot end up in it.
 */
final class AuditLog {

    /** One decision. */
    record Entry(long seq, String decision, int status, String reason, String user, String method, String path) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path file;
    private final List<Entry> entries = new ArrayList<>();

    AuditLog(Path file) throws IOException {
        this.file = file;
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.deleteIfExists(file);
    }

    synchronized Entry record(String decision, int status, String reason, String user, String method, String path) {
        Entry e = new Entry(entries.size() + 1L, decision, status, reason, user, method, path);
        entries.add(e);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("ts", Instant.now().toString());
        line.put("seq", e.seq());
        line.put("decision", e.decision());
        line.put("status", e.status());
        line.put("reason", e.reason());
        line.put("user", e.user());
        line.put("method", e.method());
        line.put("path", e.path());
        try {
            Files.writeString(file, JSON.writeValueAsString(line) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return e;
    }

    synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    String raw() throws IOException {
        return Files.exists(file) ? Files.readString(file) : "";
    }
}
