package com.jk.explore.security.threathunt;

import com.jk.explore.security.threathunt.Events.Dns;
import com.jk.explore.security.threathunt.Events.Login;
import com.jk.explore.security.threathunt.Events.Proc;
import com.jk.explore.security.threathunt.Events.Week;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** The hunts: each starts from a hypothesis about what an intruder does and asks the data for leads that a person then judges. */
public final class Hunts {

    private Hunts() {
    }

    /** A lead is a place to look. */
    public record Lead(String subject, String why) {
    }

    /** The gaps between one host's calls to one domain: how many calls, the mean gap and its standard deviation, in seconds. */
    public record Gaps(int calls, double mean, double sd) {

        /** How uneven the gaps are: the standard deviation divided by the mean. 0 is a perfect timer, about 1 is random. */
        public double variation() {
            return mean > 0 ? sd / mean : 0;
        }
    }

    /** What an account did in the first four days: the addresses it used and the hours it logged in. */
    public record Baseline(Set<String> ips, SortedSet<Integer> hours) {
    }

    /** One host's bytes out per day, its own median, the median absolute deviation (MAD) and the limit above which a day is a lead. */
    public record Volume(long[] perDay, double median, double mad, double limit) {
    }

    /** The times of the calls of each (host, domain) pair, keyed "host -> domain", sorted. */
    public static Map<String, List<Long>> callTimes(Week week) {
        Map<String, List<Long>> times = new TreeMap<>();
        for (Dns d : week.dns()) {
            times.computeIfAbsent(d.host() + " -> " + d.domain(), k -> new ArrayList<>()).add(d.at());
        }
        times.values().forEach(t -> t.sort(null));
        return times;
    }

    /** How many different hosts call each domain. */
    public static Map<String, Integer> hostsPerDomain(Week week) {
        Map<String, Set<String>> hosts = new TreeMap<>();
        for (Dns d : week.dns()) {
            hosts.computeIfAbsent(d.domain(), k -> new TreeSet<>()).add(d.host());
        }
        Map<String, Integer> counts = new TreeMap<>();
        hosts.forEach((domain, h) -> counts.put(domain, h.size()));
        return counts;
    }

    /** The gaps between sorted call times, worked out: the mean, then the standard deviation around it. */
    public static Gaps gaps(List<Long> sorted) {
        double[] gaps = new double[sorted.size() - 1];
        for (int i = 1; i < sorted.size(); i++) {
            gaps[i - 1] = sorted.get(i) - sorted.get(i - 1);
        }
        double mean = Arrays.stream(gaps).average().orElse(0);
        double sd = Math.sqrt(Arrays.stream(gaps).map(g -> (g - mean) * (g - mean)).average().orElse(0));
        return new Gaps(sorted.size(), mean, sd);
    }

    /** Hypothesis 1: malware calls home on a timer. Look for (host, domain) pairs with many calls at regular intervals. */
    public static List<Lead> beaconing(Week week, int minCalls, double maxVariation, int maxHostsForDomain, Set<String> allowlist) {
        Map<String, Integer> hostsOf = hostsPerDomain(week);
        List<Lead> leads = new ArrayList<>();
        for (var e : callTimes(week).entrySet()) {
            String domain = e.getKey().substring(e.getKey().indexOf("> ") + 2);
            if (e.getValue().size() < minCalls || allowlist.contains(domain)) {
                continue;
            }
            Gaps g = gaps(e.getValue());
            if (g.mean() > 0 && g.variation() < maxVariation && hostsOf.get(domain) <= maxHostsForDomain) {
                leads.add(new Lead(e.getKey(), g.calls() + " calls, a gap of about " + Math.round(g.mean()) + " seconds"));
            }
        }
        return leads;
    }

    /** How often each (parent, child) program pair started in the week, keyed "parent -> child". */
    public static Map<String, Integer> pairCounts(Week week) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Proc p : week.procs()) {
            counts.merge(p.parent() + " -> " + p.child(), 1, Integer::sum);
        }
        return counts;
    }

    /** Hypothesis 2: an intruder runs programs that nobody else runs. Count (parent, child) pairs and look at the rarest. */
    public static List<Lead> rarePairs(Week week, int maxCount) {
        List<Lead> leads = new ArrayList<>();
        pairCounts(week).entrySet().stream().filter(e -> e.getValue() <= maxCount).forEach(e -> leads.add(new Lead(e.getKey(), e.getValue() + " time(s) in the week")));
        return leads;
    }

    /** Each account's normal, learned from the first four days (days 0 to 3). */
    public static Map<String, Baseline> baselines(Week week) {
        Map<String, Baseline> base = new TreeMap<>();
        for (Login l : week.logins()) {
            if (l.day() < 4) {
                Baseline b = base.computeIfAbsent(l.user(), k -> new Baseline(new TreeSet<>(), new TreeSet<>()));
                b.ips().add(l.ip());
                b.hours().add(l.hour());
            }
        }
        return base;
    }

    /** Hypothesis 3: stolen accounts are used at odd hours or from new places. Compare each later login with the account's own baseline. */
    public static List<Lead> oddLogins(Week week) {
        Map<String, Baseline> base = baselines(week);
        Baseline none = new Baseline(Set.of(), new TreeSet<>());
        List<Lead> leads = new ArrayList<>();
        for (Login l : week.logins()) {
            if (l.day() < 4) {
                continue;
            }
            Baseline b = base.getOrDefault(l.user(), none);
            boolean newIp = !b.ips().contains(l.ip());
            boolean newHour = !b.hours().contains(l.hour());
            if (newIp && newHour) {
                leads.add(new Lead(l.user() + " from " + l.ip(), "a new address at an hour the account has not been seen"));
            } else if (newIp) {
                leads.add(new Lead(l.user() + " from " + l.ip(), "a new address at a usual hour"));
            }
        }
        return leads;
    }

    /** Each host's bytes out per day of the week. */
    public static Map<String, long[]> bytesPerDay(Week week) {
        Map<String, long[]> perDay = new TreeMap<>();
        for (Dns d : week.dns()) {
            perDay.computeIfAbsent(d.host(), k -> new long[7])[(int) (d.at() / Generator.DAY)] += d.bytesOut();
        }
        return perDay;
    }

    /** One host's normal, worked out: the median day, the median distance from it (MAD), and median + deviations x MAD. */
    public static Volume volumeOf(long[] perDay, double deviations) {
        double median = median(Arrays.stream(perDay).asDoubleStream().toArray());
        double mad = median(Arrays.stream(perDay).mapToDouble(v -> Math.abs(v - median)).toArray());
        return new Volume(perDay, median, mad, median + deviations * Math.max(mad, 1));
    }

    /** Hypothesis 4: data leaves in bulk. A day is a lead when it is above the host's own limit and above 50 MB. */
    public static List<Lead> volume(Week week, double deviations) {
        List<Lead> leads = new ArrayList<>();
        for (var e : bytesPerDay(week).entrySet()) {
            Volume v = volumeOf(e.getValue(), deviations);
            for (int day = 0; day < 7; day++) {
                if (v.perDay()[day] > v.limit() && v.perDay()[day] > 50_000_000L) {
                    leads.add(new Lead(e.getKey() + " on day " + day, v.perDay()[day] / 1_000_000 + " MB out against a usual " + Math.round(v.median() / 1_000_000) + " MB"));
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
