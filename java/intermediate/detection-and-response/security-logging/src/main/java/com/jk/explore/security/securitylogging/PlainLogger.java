package com.jk.explore.security.securitylogging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The bank's old logger: a timestamp, a level and whatever message the code built, with user input pasted straight in. It writes text
 * the way a file holds it, so a line break inside a message becomes a new line in the file.
 */
final class PlainLogger {

    private final SimClock clock;
    private final StringBuilder file = new StringBuilder();

    PlainLogger(SimClock clock) {
        this.clock = clock;
    }

    void info(String message) {
        write("INFO", message);
    }

    void error(String message) {
        write("ERROR", message);
    }

    void warn(String message) {
        write("WARN", message);
    }

    private void write(String level, String message) {
        file.append(clock.now()).append(' ').append(level).append(' ').append(message).append('\n');
    }

    /** The file as a reader sees it: one entry per line. */
    List<String> lines() {
        return new ArrayList<>(Arrays.asList(file.toString().split("\n")));
    }
}
