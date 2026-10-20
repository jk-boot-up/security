package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Password spraying tries one or two common passwords against <em>many</em> accounts, staying under the per-account limit that the brute-force rule watches.
 * So this rule counts distinct accounts that failed from the same address within the window.
 */
public final class SprayingRule implements Rule {

    private final int distinctUsers;
    private final Duration window;

    public SprayingRule(int distinctUsers, Duration window) {
        this.distinctUsers = distinctUsers;
        this.window = window;
    }

    @Override
    public String name() {
        return "spraying(" + distinctUsers + ")";
    }

    @Override
    public List<Alert> evaluate(List<Event> log) {
        Map<String, Deque<Event>> byIp = new HashMap<>();
        Set<String> alerted = new HashSet<>();
        List<Alert> alerts = new ArrayList<>();
        for (Event e : log) {
            if (!e.type().equals("login") || e.success()) {
                continue;
            }
            Deque<Event> q = byIp.computeIfAbsent(e.ip(), k -> new ArrayDeque<>());
            while (!q.isEmpty() && Duration.between(q.peekFirst().time(), e.time()).compareTo(window) > 0) {
                q.removeFirst();
            }
            q.addLast(e);
            long users = q.stream().map(Event::user).distinct().count();
            if (users >= distinctUsers && alerted.add(e.ip())) {
                alerts.add(new Alert(name(), e.ip(), e.time(), users + " different accounts failed from " + e.ip() + " within " + window.toMinutes() + " minutes", List.copyOf(q)));
            }
        }
        return alerts;
    }
}
