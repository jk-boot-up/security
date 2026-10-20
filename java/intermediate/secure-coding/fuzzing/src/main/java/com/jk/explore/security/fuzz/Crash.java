package com.jk.explore.security.fuzz;

/** A bug the fuzzer found: how it is recognised, the input that triggered it, and the shortest input that still does. */
public record Crash(String signature, String input, String minimized, long foundAtExecution) {
}
