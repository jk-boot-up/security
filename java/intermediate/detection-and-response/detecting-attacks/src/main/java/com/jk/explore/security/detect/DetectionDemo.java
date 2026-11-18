package com.jk.explore.security.detect;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Runs the detection story in five numbered acts and prints exact, repeatable results: each act shows its input and its result in separate, labelled sections. The
 * day of logs is synthetic, with a fixed random seed and a fixed date, so the output never changes.
 */
public class DetectionDemo {

    static final Set<String> VPN = Set.of(Scenario.VPN_EGRESS);
    static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    static final int TUNED_BRUTE_FORCE = 10;

    public static void main(String[] args) {
        System.out.print(run());
    }

    static List<Rule> first() {
        return List.of(new BruteForceRule(3, FIVE_MINUTES), new SprayingRule(8, Duration.ofMinutes(20)), new ImpossibleTravelRule(900, 500, Set.of()),
                new PrivilegeChangeRule(Scenario.APPROVERS, true), new TakeoverRule(1, Duration.ofMinutes(10), Map.of()));
    }

    static List<Rule> tuned() {
        return List.of(new BruteForceRule(TUNED_BRUTE_FORCE, FIVE_MINUTES), new SprayingRule(8, Duration.ofMinutes(20)), new ImpossibleTravelRule(900, 500, VPN),
                new PrivilegeChangeRule(Scenario.APPROVERS, true), new TakeoverRule(5, Duration.ofMinutes(10), Scenario.history()));
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        List<Event> log = Scenario.day();
        theDay(out, log);
        workedOut(out, log);
        firstVersion(out, log);
        tuning(out, log);
        tunedSet(out, log);
        return out.toString();
    }

    private static void theDay(StringBuilder out, List<Event> log) {
        long logins = log.stream().filter(e -> e.type().equals("login")).count();
        long failed = log.stream().filter(e -> e.type().equals("login") && !e.success()).count();
        long attacks = log.stream().filter(e -> e.attackId() != null).count();
        out.append("ONE. One Monday of logs.\n");
        out.append("  THE INPUT (one Monday at an online bank, 200 accounts):\n");
        out.append(String.format("    events        %d%n", log.size()));
        out.append(String.format("    logins        %d, of them %d failed%n", logins, failed));
        out.append(String.format("    role grants   %d, all of the admin role%n", log.size() - logins));
        out.append("  FOUR LINES OF THE LOG (time, type, account, address, place):\n");
        for (String[] pick : new String[][] {{"07:58:18", "u042"}, {"09:10:00", "u010"}, {"11:00:00", "u050"}, {"12:00:00", "u042"}}) {
            log.stream().filter(e -> hms(e.time()).equals(pick[0]) && e.user().equals(pick[1])).findFirst().ifPresent(e -> out.append("    ").append(line(e)).append('\n'));
        }
        out.append("  THE HIDDEN ATTACKS (the truth; no rule ever reads it):\n");
        Scenario.ATTACKS.forEach((id, name) -> {
            List<Event> events = log.stream().filter(e -> id.equals(e.attackId())).toList();
            Event a = events.getFirst();
            Event z = events.getLast();
            String when = events.size() == 1 ? hm(a.time()) : hm(a.time()) + "-" + hm(z.time());
            String from = a.type().equals("login") ? a.ip() : "by " + a.actor();
            out.append(String.format("    %s  %-17s %3d %-6s  %-11s  %s%n", id, name, events.size(), events.size() == 1 ? "event" : "events", when, from));
        });
        out.append(String.format("    %d of the %d events belong to an attack%n", attacks, log.size()));
        out.append("  THE LOOK-ALIKES (normal, but they resemble an attack):\n");
        Scenario.LOOK_ALIKES.forEach(l -> out.append(String.format("    %-19s  %s%n", l[0], l[1])));
    }

    private static void workedOut(StringBuilder out, List<Event> log) {
        out.append("TWO. Two attacks, worked out.\n");
        List<Event> sprayFails = log.stream().filter(e -> "A".equals(e.attackId()) && !e.success()).toList();
        long accounts = sprayFails.stream().map(Event::user).distinct().count();
        out.append(String.format("  THE INPUT (attack A, every failure from %s):%n", sprayFails.getFirst().ip()));
        out.append(String.format("    %d failures on %d accounts, %s to %s%n", sprayFails.size(), accounts, hms(sprayFails.getFirst().time()),
                hms(sprayFails.getLast().time())));
        int perAccount = Peaks.failuresPerAccount(sprayFails, FIVE_MINUTES).values().stream().max(Integer::compare).orElse(0);
        out.append("  PER ACCOUNT (what the brute-force rule counts):\n");
        out.append(String.format("    most failures, 1 account, 5 minutes  %d%n", perAccount));
        out.append(String.format("    %d is under %d  <- BRUTE FORCE STAYS QUIET%n", perAccount, TUNED_BRUTE_FORCE));
        Rule spraying = new SprayingRule(8, Duration.ofMinutes(20));
        Alert spray = spraying.evaluate(log).getFirst();
        List<String> seen = spray.evidence().stream().map(e -> e.user() + " " + hms(e.time())).distinct().toList();
        out.append("  PER ADDRESS (what the spraying rule counts):\n");
        out.append(String.format("    %s, %s, ... %s%n", seen.getFirst(), seen.get(1), seen.getLast()));
        out.append(String.format("    different accounts in 20 minutes  %d at %s%n", seen.size(), hms(spray.time())));
        out.append(String.format("    %d reaches 8  <- SPRAYING FIRES%n", seen.size()));

        Alert travel = new ImpossibleTravelRule(900, 500, VPN).evaluate(log).stream().filter(a -> a.evidence().get(1).attackId() != null && a.evidence().get(1).attackId().equals("C"))
                .findFirst().orElseThrow();
        out.append(String.format("  THE INPUT (attack C, %s's two successful logins):%n", travel.subject()));
        travel.evidence().forEach(e -> out.append(String.format("    %s  %-9s %s%n", hms(e.time()), e.city().name(), e.ip())));
        out.append("  THE SPEED (worked out):\n");
        speed(out, travel.evidence().get(0), travel.evidence().get(1), "  <- IMPOSSIBLE TRAVEL FIRES");
        List<Event> trip = log.stream().filter(e -> e.user().equals("u030")).toList();
        out.append(String.format("  THE NEAR MISS (u030, a real trip, %s to %s):%n", trip.get(0).city().name(), trip.get(1).city().name()));
        speed(out, trip.get(0), trip.get(1), "  <- NO ALERT");
    }

    /** The impossible-travel speed worked out with whole numbers: the distance, the minutes, and distance x 60 / minutes. */
    private static void speed(StringBuilder out, Event from, Event to, String verdict) {
        long km = Math.round(from.city().kmTo(to.city()));
        long minutes = Duration.between(from.time(), to.time()).toMinutes();
        long kmh = Math.round(km * 60.0 / minutes);
        out.append(String.format("    distance  %d km, %s to %s%n", km, from.city().name(), to.city().name()));
        out.append(String.format("    time      %d minutes, %s to %s%n", minutes, hm(from.time()), hm(to.time())));
        out.append(String.format("    speed     %d x 60 / %d = %d km/h%n", km, minutes, kmh));
        out.append(String.format("    %d is %s 900%s%n", kmh, kmh > 900 ? "over" : "under", verdict));
    }

    private static void firstVersion(StringBuilder out, List<Event> log) {
        out.append("THREE. A first version of each rule.\n");
        out.append("  THE RULES (first version; each is a guess about an attacker):\n");
        first().forEach(r -> out.append(String.format("    %-23s%s%n", r.name(), r.describe())));
        out.append("  THE RESULT (each alert scored against the hidden attacks):\n");
        Score total = table(out, first(), log);
        Map<List<String>, Integer> causes = new LinkedHashMap<>();
        for (Rule r : first()) {
            r.evaluate(log).stream().filter(a -> !a.isTruePositive()).forEach(a -> causes.merge(List.of(r.name(), Scenario.lookAlike(a.subject())), 1, Integer::sum));
        }
        out.append(String.format("  WHY %d ARE FALSE (the look-alike behind each false alert):%n", total.falsePositives()));
        causes.entrySet().stream().sorted(Map.Entry.<List<String>, Integer>comparingByValue().reversed())
                .forEach(c -> out.append(String.format("    %-23s%3d  %s%n", c.getKey().get(0), c.getValue(), c.getKey().get(1))));
    }

    private static void tuning(StringBuilder out, List<Event> log) {
        out.append("FOUR. Tuning one threshold.\n");
        Map<String, Integer> normal = Peaks.failuresPerAccount(log.stream().filter(e -> e.attackId() == null).toList(), FIVE_MINUTES);
        List<Integer> levels = normal.values().stream().distinct().sorted(Comparator.reverseOrder()).limit(2).toList();
        int worst = levels.get(0);
        out.append("  THE NORMAL DAY (most failures, 1 account, 5 minutes):\n");
        for (int level : levels) {
            List<String> who = normal.entrySet().stream().filter(x -> x.getValue() == level).map(Map.Entry::getKey).toList();
            out.append(String.format("    %-22s %2d  %s%n", String.join(", ", who), level, Scenario.lookAlike(who.getFirst())));
        }
        List<Event> brute = log.stream().filter(e -> "B".equals(e.attackId())).toList();
        int attack = Peaks.failuresPerAccount(brute, FIVE_MINUTES).values().stream().max(Integer::compare).orElse(0);
        Event success = brute.stream().filter(Event::success).findFirst().orElseThrow();
        out.append(String.format("  THE ATTACK (attack B, on %s from %s):%n", success.user(), success.ip()));
        out.append(String.format("    %-22s %2d  then a success at %s%n", success.user(), attack, hms(success.time())));
        out.append("  THE SAFE RANGE (worked out):\n");
        out.append(String.format("    above the normal day   more than %d, so at least %d%n", worst, worst + 1));
        out.append(String.format("    not above the attack   at most %d%n", attack));
        out.append(String.format("    so the threshold is    %d to %d; chosen: %d%n", worst + 1, attack, TUNED_BRUTE_FORCE));
        out.append("  THE SWEEP (the brute-force rule at each threshold):\n");
        out.append(String.format("    %9s %7s %5s %6s  %s%n", "threshold", "alerts", "true", "false", "attack B"));
        for (int t : new int[] {3, 5, 10, 25, 40}) {
            Score s = Score.of(new BruteForceRule(t, FIVE_MINUTES), log);
            out.append(String.format("    %9d %7d %5d %6d  %s%n", t, s.alerts(), s.truePositives(), s.falsePositives(),
                    s.attacksDetected().contains("B") ? "noticed" : "MISSED"));
        }
    }

    private static void tunedSet(StringBuilder out, List<Event> log) {
        out.append("FIVE. The tuned set, and what it tells the analyst.\n");
        out.append("  THE CHANGES (the three rules that get a number or context):\n");
        List<Rule> before = first();
        List<Rule> after = tuned();
        for (int i = 0; i < after.size(); i++) {
            if (!before.get(i).describe().equals(after.get(i).describe())) {
                out.append(String.format("    %-23s%s%n", base(after.get(i).name()), after.get(i).describe()));
            }
        }
        out.append("  THE RESULT (each alert scored against the hidden attacks):\n");
        table(out, after, log);
        List<Alert> alerts = after.stream().flatMap(r -> r.evaluate(log).stream()).sorted(Comparator.comparing(Alert::time)).toList();
        out.append("  THE ALERTS, IN TIME ORDER (what the analyst reads):\n");
        for (Alert a : alerts) {
            out.append(String.format("    %s  %-22s  %-12s  %s%n", hm(a.time()), base(a.rule()), a.subject(), a.isTruePositive() ? attacks(a) : "<- FALSE"));
        }
        out.append("  THE FALSE ONES (worth a look, not an attack):\n");
        alerts.stream().filter(a -> !a.isTruePositive())
                .forEach(a -> out.append(String.format("    %s  %s%n      %s%n", a.subject(), Scenario.lookAlike(a.subject()), a.summary())));
    }

    /** Prints one row per rule and a total row; returns the total. */
    private static Score table(StringBuilder out, List<Rule> rules, List<Event> log) {
        out.append(String.format("    %-30s %6s %5s %5s  %s%n", "rule", "alerts", "true", "false", "attacks"));
        int alerts = 0;
        int tp = 0;
        Set<String> found = new TreeSet<>();
        for (Rule r : rules) {
            Score s = Score.of(r, log);
            out.append(String.format("    %-30s %6d %5d %5d  %s%n", s.rule(), s.alerts(), s.truePositives(), s.falsePositives(), String.join(",", s.attacksDetected())));
            alerts += s.alerts();
            tp += s.truePositives();
            found.addAll(s.attacksDetected());
        }
        out.append(String.format("    %-30s %6d %5d %5d  %s%n", "together", alerts, tp, alerts - tp, String.join(",", found)));
        return new Score("together", alerts, tp, found);
    }

    private static String attacks(Alert a) {
        return a.evidence().stream().map(Event::attackId).filter(x -> x != null).distinct().sorted().collect(Collectors.joining(" "));
    }

    /** A rule's name without its numbers and context: "brute-force(10)" is "brute-force". */
    static String base(String rule) {
        return rule.replaceAll("[(+].*", "");
    }

    private static String line(Event e) {
        if (e.type().equals("login")) {
            return String.format("%s  login  %s  %-12s  %-9s  %s", hms(e.time()), e.user(), e.ip(), e.city().name(), e.success() ? "success" : "failure");
        }
        return String.format("%s  grant  %s  %s  by %s  ticket %s", hms(e.time()), e.user(), e.role(), e.actor(), e.ticket().isEmpty() ? "none" : e.ticket());
    }

    private static String hms(Instant t) {
        return t.toString().substring(11, 19);
    }

    private static String hm(Instant t) {
        return t.toString().substring(11, 16);
    }
}
