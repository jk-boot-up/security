package com.jk.explore.security.securitylogging;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bank's security log. Every event becomes one JSON line with a fixed set of fields, its details pass through the redactor, and
 * each line carries the SHA-256 of the line before it ("prev"), so a deleted or edited line breaks the chain.
 */
final class SecurityLog {

    /** The "previous hash" of the very first line. */
    static final String GENESIS = "0".repeat(64);

    private final List<LogSink> sinks;
    private String head;

    SecurityLog(LogSink... sinks) {
        this(GENESIS, sinks);
    }

    SecurityLog(String anchor, LogSink... sinks) {
        this.sinks = List.of(sinks);
        this.head = anchor;
    }

    String record(SecurityEvent event) throws IOException {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("prev", head);
        fields.put("time", event.time().toString());
        fields.put("type", event.type().label());
        fields.put("actor", Redactor.text(event.actor()));
        fields.put("action", Redactor.text(event.action()));
        fields.put("target", Redactor.text(event.target()));
        fields.put("source", event.source());
        fields.put("outcome", event.outcome().label());
        fields.put("correlation", event.correlation());
        Redactor.details(event.details()).forEach((name, value) -> fields.put("detail." + name, value));
        String line = Json.write(fields);
        for (LogSink sink : sinks) {
            sink.write(line);
        }
        head = Hashes.sha256(line);
        return line;
    }

    /** The hash of the latest line: published somewhere else, it anchors the end of the chain. */
    String head() {
        return head;
    }
}
