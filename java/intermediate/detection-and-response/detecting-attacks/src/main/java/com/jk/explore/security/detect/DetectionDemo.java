package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Runs the detection story in four numbered acts and prints exact, repeatable results. The day of logs is synthetic, with a fixed random seed. */
public class DetectionDemo {

    static final Set<String> VPN = Set.of(Scenario.VPN_EGRESS);

    public static void main(String[] args) {
        System.out.print(run());
    }

    static List<Rule> tuned() {
        return List.of(new BruteForceRule(10, Duration.ofMinutes(5)), new SprayingRule(8, Duration.ofMinutes(20)), new ImpossibleTravelRule(900, 500, VPN),
                new PrivilegeChangeRule(Scenario.APPROVERS, true), new TakeoverRule(5, Duration.ofMinutes(10), Scenario.history()));
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        List<Event> log = Scenario.day();
        long attackEvents = log.stream().filter(e -> e.attackId() != null).count();
        long logins = log.stream().filter(e -> e.type().equals("login")).count();

        out.append("== 1. One Monday of logs ==\n");
        out.append(log.size()).append(" events: ").append(logins).append(" logins, ").append(log.size() - logins).append(" role grants; ").append(attackEvents)
                .append(" of them belong to 4 real attacks (A: spraying, B: brute force, C: impossible travel, D: privilege change)\n");

        out.append("\n== 2. A first version of each rule ==\n");
        List<Rule> first = List.of(new BruteForceRule(3, Duration.ofMinutes(5)), new SprayingRule(8, Duration.ofMinutes(20)), new ImpossibleTravelRule(900, 500, Set.of()),
                new PrivilegeChangeRule(Scenario.APPROVERS, true), new TakeoverRule(1, Duration.ofMinutes(10), java.util.Map.of()));
        header(out);
        first.forEach(r -> row(out, Score.of(r, log)));

        out.append("\n== 3. Tuning one threshold ==\n");
        header(out);
        for (int t : new int[] {3, 5, 10, 25, 40}) {
            row(out, Score.of(new BruteForceRule(t, Duration.ofMinutes(5)), log));
        }

        out.append("\n== 4. The tuned set, and what it tells the analyst ==\n");
        header(out);
        int alerts = 0;
        int tp = 0;
        Set<String> found = new java.util.TreeSet<>();
        for (Rule r : tuned()) {
            Score s = Score.of(r, log);
            row(out, s);
            alerts += s.alerts();
            tp += s.truePositives();
            found.addAll(s.attacksDetected());
        }
        out.append("together: ").append(alerts).append(" alerts for the day, ").append(tp).append(" true, ").append(alerts - tp).append(" false; attacks noticed: ").append(found).append(" of [A, B, C, D]\n");
        out.append("the alerts, in time order:\n");
        tuned().stream().flatMap(r -> r.evaluate(log).stream()).sorted(java.util.Comparator.comparing(Alert::time)).forEach(a ->
                out.append(String.format("  %s %-22s %s%s%n", a.time().toString().substring(11, 16), a.rule(), a.summary(), a.isTruePositive() ? "" : "   <- FALSE POSITIVE")));
        return out.toString();
    }

    private static void header(StringBuilder out) {
        out.append(String.format("%-28s %6s %5s %5s  %s%n", "rule", "alerts", "true", "false", "attacks noticed"));
    }

    private static void row(StringBuilder out, Score s) {
        out.append(String.format("%-28s %6d %5d %5d  %s%n", s.rule(), s.alerts(), s.truePositives(), s.falsePositives(), s.attacksDetected()));
    }
}
