package com.jk.explore.security.leaks;

/**
 * A value that must never be printed: its {@code toString} says so. Code that needs the value calls {@link #reveal()} at the one place it is used, which is
 * easy to search for and to review. Putting a {@code Secret} in a log message, an exception message or a JSON field leaks nothing.
 */
public final class Secret {

    private final String value;

    public Secret(String value) {
        this.value = value;
    }

    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
