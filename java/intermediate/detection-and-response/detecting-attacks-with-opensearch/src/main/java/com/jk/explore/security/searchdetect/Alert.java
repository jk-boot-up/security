package com.jk.explore.security.searchdetect;

import java.time.Instant;
import java.util.List;

/** What a rule reports: which rule, about whom, when, and the events that made it fire. */
public record Alert(String rule, String subject, Instant time, String summary, List<Event> evidence) {

    /** True when at least one event behind the alert belongs to a real attack (scoring only). */
    public boolean isTruePositive() {
        return evidence.stream().anyMatch(e -> e.attackId() != null);
    }
}
