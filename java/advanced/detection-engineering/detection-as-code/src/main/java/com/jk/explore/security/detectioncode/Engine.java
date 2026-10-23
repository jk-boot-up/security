package com.jk.explore.security.detectioncode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs a rule over events in time order and returns the ids of the events that raise an alert. */
final class Engine {

    private Engine() {
    }

    static List<Integer> alerts(Rule rule, Map<String, Rule> all, List<Ev> events) {
        if (rule.correlation() == null) {
            return events.stream().filter(e -> rule.matches(e.f())).map(Ev::id).toList();
        }
        Rule.Correlation c = rule.correlation();
        return count(c, events.stream().filter(e -> all.get(c.base()).matches(e.f())).toList());
    }

    /**
     * The correlation on the events the base rule matched: when gte of them fall within the span for one group the last one raises an alert,
     * and the group is then quiet for one span, so one campaign is one alert and not one for every ten events.
     */
    static List<Integer> count(Rule.Correlation c, List<Ev> matched) {
        Map<String, Deque<Long>> window = new HashMap<>();
        Map<String, Long> quietUntil = new HashMap<>();
        List<Integer> out = new ArrayList<>();
        long span = c.spanSeconds() * 1000;
        for (Ev e : matched) {
            String key = String.join("|", c.groupBy().stream().map(g -> e.f().getOrDefault(g, "")).toList());
            if (quietUntil.getOrDefault(key, Long.MIN_VALUE) > e.ms()) {
                continue;
            }
            Deque<Long> times = window.computeIfAbsent(key, k -> new ArrayDeque<>());
            times.addLast(e.ms());
            while (e.ms() - times.peekFirst() > span) {
                times.removeFirst();
            }
            if (times.size() >= c.gte()) {
                out.add(e.id());
                times.clear();
                quietUntil.put(key, e.ms() + span);
            }
        }
        return out;
    }
}
