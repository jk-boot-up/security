package com.jk.explore.security.detectioncode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One rule against the truth. An attack is detected when the rule alerts on at least one of its events (true positive), missed otherwise
 * (false negative). A false positive is an alert on a benign event. An alert on another rule's attack is neither.
 */
record Score(String rule, int attacks, int detected, int missed, int tpAlerts, int falsePositives, int otherAlerts, int[] perDay, int[] falsePerDay, Set<Integer> detectedInstances) {

    static Score of(String name, Map<String, Rule> rules, Corpus c) {
        return of(name, Engine.alerts(rules.get(name), rules, c.events()), c);
    }

    static Score of(String name, List<Integer> alerts, Corpus c) {
        Set<Integer> instances = new HashSet<>();
        Set<Integer> hit = new HashSet<>();
        for (Truth t : c.truth().values()) {
            if (t.attack() && t.rule().equals(name)) {
                instances.add(t.instance());
            }
        }
        int tp = 0;
        int fp = 0;
        int other = 0;
        int[] perDay = new int[Corpus.DAYS];
        int[] falsePerDay = new int[Corpus.DAYS];
        for (int id : alerts) {
            Truth t = c.truth().get(id);
            perDay[Corpus.day(c.events().get(id - 1))]++;
            if (t.benign()) {
                fp++;
                falsePerDay[Corpus.day(c.events().get(id - 1))]++;
            } else if (t.rule().equals(name)) {
                tp++;
                hit.add(t.instance());
            } else {
                other++;
            }
        }
        return new Score(name, instances.size(), hit.size(), instances.size() - hit.size(), tp, fp, other, perDay, falsePerDay, hit);
    }

    double precision() {
        return tpAlerts + falsePositives == 0 ? 1.0 : (double) tpAlerts / (tpAlerts + falsePositives);
    }

    double recall() {
        return attacks == 0 ? 1.0 : (double) detected / attacks;
    }

    int alerts() {
        return tpAlerts + falsePositives + otherAlerts;
    }

    int worstDay() {
        int w = 0;
        for (int n : falsePerDay) {
            w = Math.max(w, n);
        }
        return w;
    }

    double meanPerDay() {
        return (double) falsePositives / falsePerDay.length;
    }

    int daysOver(int budget) {
        int n = 0;
        for (int d : falsePerDay) {
            if (d > budget) {
                n++;
            }
        }
        return n;
    }
}
