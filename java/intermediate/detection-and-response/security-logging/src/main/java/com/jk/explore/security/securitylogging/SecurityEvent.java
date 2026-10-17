package com.jk.explore.security.securitylogging;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One security event: who did what, to what, when, from where, how it ended, and the request it belongs to. Details hold anything
 * else the application knows, including values that must never be written down; the log's redactor decides what survives.
 */
record SecurityEvent(Instant time, EventType type, String actor, String action, String target, String source, Outcome outcome,
        String correlation, Map<String, String> details) {

    SecurityEvent {
        details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
