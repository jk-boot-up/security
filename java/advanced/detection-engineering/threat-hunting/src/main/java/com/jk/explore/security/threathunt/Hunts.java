package com.jk.explore.security.threathunt;

import com.jk.explore.security.threathunt.Events.Dns;
import com.jk.explore.security.threathunt.Events.Login;
import com.jk.explore.security.threathunt.Events.Proc;
import com.jk.explore.security.threathunt.Events.Week;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** The hunts: each starts from a hypothesis about what an intruder does and asks the data for leads that a person then judges. */
public final class Hunts {

    private Hunts() {
    }

    /** A lead is a place to look. */
    public record Lead(String subject, String why) {
    }

    /** Hypothesis 1: malware calls home on a timer. Look for (host, domain) pairs with many calls at regular intervals. */
    public static List<Lead> beaconing(Week week, int minCalls, double maxVariation, int maxHostsForDomain, Set<String> allowlist) {
        Map<String, List<Long>> times = new TreeMap<>();
        Map<String, Set<String>> hostsOf = new HashMap<>();
        for (Dns d : week.dns()) {
            times.computeIfAbsent(d.host() + " -> " + d.domain(), k -> new ArrayList<>()).add(d.at());
            hostsOf.computeIfAbsent(d.domain(), k -> new HashSet<>()).add(d.host());
        }
        List<Lead> leads = new ArrayList<>();
        for (var e : times.entrySet()) {
            String domain = e.getKey().substring(e.getKey().indexOf("> ") + 2);
            List<Long> t = e.getValue();
            if (t.size() < minCalls || allowlist.contains(domain)) {
                continue;
            }
            t.sort(null);
            double[] gaps = new double[t.size() - 1];
            for (int i = 1; i < t.size(); i++) {
                gaps[i - 1] = t.get(i) - t.get(i - 1);
            }
            double mean = Arrays.stream(gaps).average().orElse(0);
            double sd = Math.sqrt(Arrays.stream(gaps).map(g -> (g - mean) * (g - mean)).average().orElse(0));
            if (mean > 0 && sd / mean < maxVariation && hostsOf.get(domain).size() <= maxHostsForDomain) {
                leads.add(new Lead(e.getKey(), t.size() + " calls, a gap of about " + Math.round(mean) + " seconds"));
            }
        }
        return leads;
    }

    /** Hypothesis 2: an intruder runs programs that nobody else runs. Count (parent, child) pairs and look at the rarest. */
    public static List<Lead> rarePairs(Week week, int maxCount) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Proc p : week.procs()) {
            counts.merge(p.parent() + " -> " + p.child(), 1, Integer::sum);
        }
        List<Lead> leads = new ArrayList<>();
        counts.entrySet().stream().filter(e -> e.getValue() <= maxCount).forEach(e -> leads.add(new Lead(e.getKey(), e.getValue() + " time(s) in the week")));
        return leads;
    }

    /** Hypothesis 3: stolen accounts are used at odd hours or from new places. Compare each login with the account's own history. */
    public static List<Lead> oddLogins(Week week) {
        Map<String, Set<String>> seenIps = new HashMap<>();
        Map<String, int[]> hours = new HashMap<>();
        for (Login l : week.logins()) {
            if (l.day() < 4) {
                seenIps.computeIfAbsent(l.user(), k -> new HashSet<>()).add(l.ip());
                hours.computeIfAbsent(l.user(), k -> new int[24])[l.hour()]++;
            }
        }
        List<Lead> leads = new ArrayList<>();
        for (Login l : week.logins()) {
            if (l.day() < 4) {
                continue;
            }
            boolean newIp = !seenIps.getOrDefault(l.user(), Set.of()).contains(l.ip());
            boolean newHour = hours.getOrDefault(l.user(), new int[24])[l.hour()] == 0;
            if (newIp && newHour) {
                leads.add(new Lead(l.user() + " from " + l.ip(), "a new address at an hour the account has not been seen"));
            } else if (newIp) {
                leads.add(new Lead(l.user() + " from " + l.ip(), "a new address at a usual hour"));
            }
        }
        return leads;
    }

    /** Hypothesis 4: data leaves in bulk. Compare each host's day with its own median, in units of the median absolute deviation. */
    public static List<Lead> volume(Week week, double deviations) {
        Map<String, long[]> perDay = new TreeMap<>();
        for (Dns d : week.dns()) {
            perDay.computeIfAbsent(d.host(), k -> new long[7])[(int) (d.at() / Generator.DAY)] += d.bytesOut();
        }
        List<Lead> leads = new ArrayList<>();
        for (var e : perDay.entrySet()) {
            double median = median(Arrays.stream(e.getValue()).asDoubleStream().toArray());
            double mad = median(Arrays.stream(e.getValue()).mapToDouble(v -> Math.abs(v - median)).toArray());
            for (int day = 0; day < 7; day++) {
                if (e.getValue()[day] > median + deviations * Math.max(mad, 1) && e.getValue()[day] > 50_000_000L) {
                    leads.add(new Lead(e.getKey() + " on day " + day, e.getValue()[day] / 1_000_000 + " MB out against a usual " + Math.round(median / 1_000_000) + " MB"));
                }
            }
        }
        return leads;
    }

    static double median(double[] v) {
        double[] s = v.clone();
        Arrays.sort(s);
        return s.length % 2 == 1 ? s[s.length / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2;
    }

    /** The subjects of the leads, sorted, for the scoreboard. */
    public static Set<String> subjects(List<Lead> leads) {
        return new TreeSet<>(leads.stream().map(Lead::subject).toList());
    }

    public static Comparator<Lead> bySubject() {
        return Comparator.comparing(Lead::subject);
    }
}
