package com.jk.explore.security.secretscan;

/** Something a scanner reported: the rule, the file and the text it matched. */
public record Finding(String rule, String file, String text) {
}
