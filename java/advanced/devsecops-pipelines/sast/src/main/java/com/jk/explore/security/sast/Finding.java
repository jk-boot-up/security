package com.jk.explore.security.sast;

/** One result of a scan: the rule, the file name and the line. */
public record Finding(String rule, String file, int line) {

    String key() {
        return file + ":" + line;
    }
}
