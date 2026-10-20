package com.jk.explore.security.detect;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** How a rule did against the ground truth: alerts, true and false positives, and which attacks it noticed at all. */
public record Score(String rule, int alerts, int truePositives, Set<String> attacksDetected) {

    public int falsePositives() {
        return alerts - truePositives;
    }

    public static Score of(Rule rule, List<Event> log) {
        List<Alert> alerts = rule.evaluate(log);
        Set<String> found = new TreeSet<>();
        int tp = 0;
        for (Alert a : alerts) {
            if (a.isTruePositive()) {
                tp++;
                a.evidence().stream().map(Event::attackId).filter(x -> x != null).forEach(found::add);
            }
        }
        return new Score(rule.name(), alerts.size(), tp, found);
    }
}
