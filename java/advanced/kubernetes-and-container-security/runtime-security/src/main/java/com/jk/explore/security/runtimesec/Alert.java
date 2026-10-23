package com.jk.explore.security.runtimesec;

/** One finding: which rule, in which pod, and what was seen. */
public record Alert(String rule, String pod, String detail) {
}
