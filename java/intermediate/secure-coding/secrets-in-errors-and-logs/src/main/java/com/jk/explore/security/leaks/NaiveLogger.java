package com.jk.explore.security.leaks;

/** The first version: the message is written as it comes, including line breaks that a customer typed, and the exception's message and trace. */
public final class NaiveLogger implements Logger {

    private final LogSink sink;

    public NaiveLogger(LogSink sink) {
        this.sink = sink;
    }

    @Override
    public void info(String message) {
        sink.write("INFO " + message);
        // a message holding "\n" is written as two lines, and the second one can look like anything
    }

    @Override
    public void error(String message, Throwable cause) {
        sink.write("ERROR " + message + " | " + cause);
    }
}
