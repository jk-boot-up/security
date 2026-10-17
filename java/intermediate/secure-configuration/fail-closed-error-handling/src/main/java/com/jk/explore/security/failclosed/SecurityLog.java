package com.jk.explore.security.failclosed;

import java.util.ArrayList;
import java.util.List;

/** The bank's internal log. It gets the detail the customer must not see, but never a full card number or a secret. */
final class SecurityLog {

    private final List<String> lines = new ArrayList<>();

    void write(String line) {
        lines.add(redact(line));
    }

    List<String> lines() {
        return List.copyOf(lines);
    }

    /** Any run of thirteen to nineteen digits is a card number, and keeps only its last four digits. */
    static String redact(String text) {
        return text.replaceAll("\\b\\d{9,15}(\\d{4})\\b", "****$1");
    }
}
