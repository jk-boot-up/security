package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** At least {@code threshold} failed logins for one account within {@code window}: someone is guessing that account's password. */
public final class BruteForceRule implements Rule {

    private final int threshold;
    private final Duration window;

    public BruteForceRule(int threshold, Duration window) {
        this.threshold = threshold;
        this.window = window;
    }

    @Override
    public String name() {
        return "brute-force(" + threshold + ")";
    }

    @Override
    public List<Alert> evaluate(List<Event> log) {
        Map<String, Deque<Event>> recent = new HashMap<>();
        Map<String, Boolean> firing = new HashMap<>();
        List<Alert> alerts = new ArrayList<>();
        for (Event e : log) {
            if (!e.type().equals("login")) {
                continue;
            }
            Deque<Event> q = recent.computeIfAbsent(e.user(), k -> new ArrayDeque<>());
            while (!q.isEmpty() && Duration.between(q.peekFirst().time(), e.time()).compareTo(window) > 0) {
                q.removeFirst();
            }
            if (e.success()) {
                q.clear();
                firing.put(e.user(), false);
                continue;
            }
            q.addLast(e);
            if (q.size() >= threshold && !firing.getOrDefault(e.user(), false)) {
                firing.put(e.user(), true); // one alert for a run of failures, not one per failure
                alerts.add(new Alert(name(), e.user(), e.time(), q.size() + " failed logins for " + e.user() + " within " + window.toMinutes() + " minutes", List.copyOf(q)));
            }
        }
        return alerts;
    }
}
