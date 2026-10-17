package com.jk.explore.security.securitylogging;

import java.util.ArrayList;
import java.util.List;

/** Keeps lines in memory: the demo's view of a log, and the second store that shipped lines arrive in. */
final class MemorySink implements LogSink {

    private final List<String> lines = new ArrayList<>();

    @Override
    public void write(String line) {
        lines.add(line);
    }

    List<String> lines() {
        return List.copyOf(lines);
    }

    void clear() {
        lines.clear();
    }
}
