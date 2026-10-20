package com.jk.explore.security.leaks;

/**
 * The fixed version. Every line is one line: control characters in customer-supplied text become visible escapes (so a customer cannot forge a log entry),
 * the line is redacted and cut to a sane length, and exceptions are written as class name plus a redacted message, linked to the reference given to the customer.
 */
public final class SafeLogger implements Logger {

    static final int MAX_LINE = 300;

    private final LogSink sink;
    private final Redactor redactor = new Redactor();

    public SafeLogger(LogSink sink) {
        this.sink = sink;
    }

    @Override
    public void info(String message) {
        sink.write("INFO " + clean(message));
    }

    @Override
    public void error(String message, Throwable cause) {
        sink.write("ERROR " + clean(message) + " | " + cause.getClass().getSimpleName() + ": " + clean(String.valueOf(cause.getMessage())));
    }

    String clean(String text) {
        StringBuilder b = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else if (c == '\t') {
                b.append("\\t");
            } else if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029') {
                b.append(String.format("\\u%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        String line = redactor.redact(b.toString());
        return line.length() > MAX_LINE ? line.substring(0, MAX_LINE) + "...[cut]" : line;
    }
}
