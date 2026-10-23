package com.jk.explore.security.archreview;

/** What a rule reports: the rule, the part or flow it is about, and a sentence. */
public record Finding(String rule, String subject, String message) {
}
