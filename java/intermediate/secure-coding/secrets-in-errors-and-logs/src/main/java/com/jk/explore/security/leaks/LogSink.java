package com.jk.explore.security.leaks;

import java.util.ArrayList;
import java.util.List;

/** Where log lines end up. In production this is a file and then a log service that many people can search; here it is a list the demo and the tests can read. */
public final class LogSink {

    private final List<String> lines = new ArrayList<>();

    public void write(String line) {
        lines.add(line);
    }

    public List<String> lines() {
        return List.copyOf(lines);
    }

    public boolean anyContains(String text) {
        return lines.stream().anyMatch(l -> l.contains(text));
    }
}
