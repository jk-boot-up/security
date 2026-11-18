package com.jk.explore.security.racepg;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The PostgreSQL error codes (SQLSTATE) this lesson meets, and what the application should do about each one. The first two
 * characters are the class: 23 is a broken integrity rule, 55 an object that is not ready, 40 a transaction the database rolled back.
 */
final class SqlStates {

    record Meaning(String name, String action, boolean retry) {
    }

    static final Map<String, Meaning> KNOWN = new LinkedHashMap<>();

    static {
        KNOWN.put("23514", new Meaning("check_violation", "refuse, do not retry", false));
        KNOWN.put("55P03", new Meaning("lock_not_available", "try again later", false));
        KNOWN.put("40001", new Meaning("serialization_failure", "retry the transaction", true));
        KNOWN.put("40P01", new Meaning("deadlock_detected", "retry the transaction", true));
    }

    private SqlStates() {
    }

    static Meaning of(String code) {
        return KNOWN.getOrDefault(code, new Meaning("unknown", "report it", false));
    }

    /** Whether the whole transaction should simply be run again: the database rolled it back only because of timing. */
    static boolean retry(String code) {
        return of(code).retry();
    }

    /** A code with its name, such as "40001 serialization_failure". */
    static String named(String code) {
        return code + " " + of(code).name();
    }
}
