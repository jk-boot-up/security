package com.jk.explore.security.logging;

/** A value that is never printed: logging it, or putting it in a message or a string, shows a marker. The real value is read only by the code that needs it. */
public record Secret(String value) {

    @Override
    public String toString() {
        return "[REDACTED]";
    }

    /** The only way out, and it has a name that is easy to search for. */
    public String reveal() {
        return value;
    }
}
