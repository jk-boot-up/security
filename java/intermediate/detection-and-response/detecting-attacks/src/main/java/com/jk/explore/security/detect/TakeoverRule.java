package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A success right after a run of failures, from an address that has never succeeded for this account before: the guesses worked.
 * Failures followed by a success from the account's usual address (a customer who finally remembered) are ignored.
 */
public final class TakeoverRule implements Rule {

    private final int failures;
    private final Duration window;
    private final Map<String, Set<String>> history;

    /** {@code history} gives each account's usual addresses from earlier days; with none, the first address seen today counts as new. */
    public TakeoverRule(int failures, Duration window, Map<String, Set<String>> history) {
        this.failures = failures;
        this.window = window;
        this.history = history;
    }

    @Override
    public String name() {
        return "success-after-failures" + (history.isEmpty() ? "" : "+history");
    }

    @Override
    public List<Alert> evaluate(List<Event> log) {
        Map<String, List<Event>> fails = new HashMap<>();
        Map<String, Set<String>> knownIps = new HashMap<>();
        List<Alert> alerts = new ArrayList<>();
        for (Event e : log) {
            if (!e.type().equals("login")) {
                continue;
            }
            List<Event> f = fails.computeIfAbsent(e.user(), k -> new ArrayList<>());
            if (!e.success()) {
                f.add(e);
                continue;
            }
            f.removeIf(x -> Duration.between(x.time(), e.time()).compareTo(window) > 0);
            Set<String> known = knownIps.computeIfAbsent(e.user(), k -> new HashSet<>(history.getOrDefault(k, Set.of())));
            long fromThisIp = f.stream().filter(x -> x.ip().equals(e.ip())).count();
            if (fromThisIp >= failures && !known.contains(e.ip())) {
                List<Event> evidence = new ArrayList<>(f);
                evidence.add(e);
                alerts.add(new Alert(name(), e.user(), e.time(), "login succeeded from " + e.ip() + " after " + fromThisIp + " failures from the same address", evidence));
            }
            known.add(e.ip());
            f.clear();
        }
        return alerts;
    }
}
