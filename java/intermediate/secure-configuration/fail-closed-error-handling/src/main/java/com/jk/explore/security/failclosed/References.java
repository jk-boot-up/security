package com.jk.explore.security.failclosed;

/** Reference numbers that a customer can quote and support can find in the log. Counted, so the demo is repeatable. */
final class References {

    private int next = 1;

    String next() {
        return String.format(java.util.Locale.ROOT, "REF-%04d", next++);
    }
}
