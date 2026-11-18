package com.jk.explore.security.secretscan;

/** Something a scanner reported: the rule, the file, the text it matched, and the commit it was added in (empty for a scan of files). */
public record Finding(String rule, String file, String text, String commit) {

    public Finding(String rule, String file, String text) {
        this(rule, file, text, "");
    }
}
