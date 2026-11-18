package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Measures a log before a threshold is chosen: the most failed logins each account had within a window, counted the way {@link BruteForceRule} counts them
 * (a success starts the count again). A brute-force threshold above the normal day's peak and at most the attack's peak catches the attack and nothing else.
 */
public final class Peaks {

    private Peaks() {
    }

    public static Map<String, Integer> failuresPerAccount(List<Event> log, Duration window) {
        Map<String, Deque<Event>> recent = new TreeMap<>();
        Map<String, Integer> peak = new TreeMap<>();
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
                continue;
            }
            q.addLast(e);
            peak.merge(e.user(), q.size(), Math::max);
        }
        return peak;
    }
}
