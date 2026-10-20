package com.jk.explore.security.leaks;

/** Writes log lines for the shop. The two implementations differ in what they do to a message before it is written. */
public interface Logger {

    void info(String message);

    void error(String message, Throwable cause);
}
