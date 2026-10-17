package com.jk.explore.security.threats;

/** One way into the system, such as a web address the store answers, and the data behind it. */
public record EntryPoint(String route, Exposure exposure, String data) {
}
