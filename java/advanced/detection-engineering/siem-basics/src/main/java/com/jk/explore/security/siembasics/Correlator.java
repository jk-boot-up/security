package com.jk.explore.security.siembasics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rules that look across sources and across time. */
public final class Correlator {

    /** An alert: which rule, about whom, and how many events stand behind it. */
    public record Alert(String rule, String user, int events) {
    }

    private Correlator() {
    }

    /** A burst of failed logins for one user from one address, across all the sources together, followed by a success from the same address. */
    public static List<Alert> bruteForce(List<Event> events, int minFailures, Duration window) {
        List<Alert> alerts = new ArrayList<>();
        Map<String, List<Event>> byPair = events.stream().filter(e -> e.action().equals("login")).collect(Collectors.groupingBy(e -> e.user() + "|" + e.ip(), TreeMap::new, Collectors.toList()));
        for (var entry : byPair.entrySet()) {
            List<Event> list = entry.getValue();
            list.sort(Comparator.comparing(Event::at));
            for (Event ok : list) {
                if (!ok.outcome().equals("success")) {
                    continue;
                }
                long failures = list.stream().filter(Event::failedLogin).filter(f -> !f.at().isAfter(ok.at()) && Duration.between(f.at(), ok.at()).compareTo(window) <= 0).count();
                if (failures >= minFailures) {
                    alerts.add(new Alert("burst of failures, then a success", ok.user(), (int) failures + 1));
                    break;
                }
            }
        }
        return alerts;
    }

    /** A user who has just logged in on the web and then reads a very large number of rows from the database within the window. */
    public static List<Alert> loginThenBulkRead(List<Event> events, Duration window) {
        List<Alert> alerts = new ArrayList<>();
        for (Event read : events) {
            if (!read.action().equals("bulk-read")) {
                continue;
            }
            boolean login = events.stream().anyMatch(e -> e.source().equals("web") && e.action().equals("login") && e.outcome().equals("success") && e.user().equals(read.user())
                    && !e.at().isAfter(read.at()) && Duration.between(e.at(), read.at()).compareTo(window) <= 0);
            if (login) {
                alerts.add(new Alert("login, then a bulk read", read.user(), 2));
            }
        }
        return alerts;
    }

    /** Failures per source only: the same rule as {@link #bruteForce} but each source on its own, as if the sources were not joined. */
    public static List<Alert> perSource(List<Event> events, int minFailures, Duration window) {
        List<Alert> alerts = new ArrayList<>();
        for (String source : Set.of("web", "host")) {
            alerts.addAll(bruteForce(events.stream().filter(e -> e.source().equals(source)).toList(), minFailures, window));
        }
        return alerts;
    }

    /** Alerts about the same user are one case. */
    public static Map<String, List<Alert>> cases(List<Alert> alerts) {
        return alerts.stream().collect(Collectors.groupingBy(Alert::user, TreeMap::new, Collectors.toList()));
    }
}
