package com.jk.explore.security.securitylogging;

import java.util.List;
import java.util.Map;

/** The questions an investigator asks of a structured log, answered by field instead of by guessing at text. */
final class LogQuery {

    private LogQuery() {
    }

    static List<Map<String, String>> where(List<String> lines, String field, String value) {
        return lines.stream().map(Json::parse).filter(f -> value.equals(f.get(field))).toList();
    }

    static long plainLinesMentioning(List<String> lines, String text) {
        return lines.stream().filter(l -> l.contains(text)).count();
    }
}
