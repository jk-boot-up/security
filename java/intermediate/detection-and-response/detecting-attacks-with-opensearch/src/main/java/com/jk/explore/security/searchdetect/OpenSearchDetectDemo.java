package com.jk.explore.security.searchdetect;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

public class OpenSearchDetectDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            OpenSearch os = new OpenSearch(lab.url());
            os.createAuditIndex("audit");
            List<Event> day = Scenario.day();
            os.bulk("audit", day);
            act1(out, os, day);
            act2(out, os);
            act3(out, os);
            act4(out, os);
            act5(out, os);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void header(StringBuilder out) {
        line(out, String.format("%-28s %6s %5s %5s  %s", "rule", "alerts", "true", "false", "attacks noticed"));
    }

    private static Set<String> attacks(List<Hit> hits) {
        Set<String> out = new TreeSet<>();
        hits.forEach(h -> out.addAll(h.attacks()));
        return out;
    }

    private static void row(StringBuilder out, String name, List<Hit> hits) {
        long tp = hits.stream().filter(Hit::truePositive).count();
        line(out, String.format("%-28s %6d %5d %5d  %s", name, hits.size(), tp, hits.size() - tp, attacks(hits)));
    }

    private static void act1(StringBuilder out, OpenSearch os, List<Event> day) throws Exception {
        line(out, "ONE. One Monday of logs, in OpenSearch.");
        long logins = os.count("audit", Map.of("term", Map.of("type", "login")));
        long grants = os.count("audit", Map.of("term", Map.of("type", "role-grant")));
        long labelled = os.count("audit", Map.of("exists", Map.of("field", "attack")));
        line(out, "  " + day.size() + " events loaded with the bulk API: " + logins + " logins, " + grants + " role grants; " + labelled + " of them belong to 4 real attacks (A: spraying, B: brute force, C: impossible travel, D: privilege change)");
        line(out, "  the attack label is in the index only so that this lesson can score its rules; no rule reads it");
        line(out, "  the fields the rules filter and group by are keywords and the time is a date, set by a mapping before the first event");
    }

    private static void act2(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "TWO. A first version of each rule, as a query.");
        header(out);
        row(out, "brute-force(3)", Detections.run(os, "audit", Detections.bruteForce(3)));
        row(out, "spraying(8)", Detections.run(os, "audit", Detections.spraying(8)));
        row(out, "privilege-change", Detections.run(os, "audit", Detections.privilegeChange()));
        row(out, "success-after-failures(1)", Detections.run(os, "audit", Detections.successAfterFailures(1)));
        line(out, "  a rule is a query: a filter, a group by the subject, a bucket of time, and a threshold that keeps the buckets worth looking at");
    }

    private static void act3(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "THREE. Tuning one threshold.");
        header(out);
        for (int t : new int[] {3, 5, 10, 25, 40}) {
            row(out, "brute-force(" + t + ")", Detections.run(os, "audit", Detections.bruteForce(t)));
        }
        line(out, "  changing the threshold is changing one number in the query; the table says what it costs and what it misses");
    }

    private static void act4(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "FOUR. The tuned set as monitors of the alerting plugin.");
        List<Hit> all = new ArrayList<>();
        header(out);
        String[] names = {"brute-force(10)", "spraying(8)", "success-after-failures(20)", "privilege-change"};
        List<Map<String, Object>> monitors = List.of(Monitors.bruteForce(10), Monitors.spraying(8), Monitors.successAfterFailures(20), Monitors.privilegeChange());
        List<String[]> alerts = new ArrayList<>();
        for (int i = 0; i < monitors.size(); i++) {
            String id = os.createMonitor(monitors.get(i));
            JsonNode run = os.execute(id);
            List<Hit> hits;
            if (i < 3) {
                hits = Monitors.triggered(run);
            } else {
                hits = run.get("trigger_results").propertyStream().findFirst().orElseThrow().getValue().get("triggered").asBoolean() ? Detections.run(os, "audit", Detections.privilegeChange()) : List.of();
            }
            row(out, names[i], hits);
            all.addAll(hits);
            for (Hit h : hits) {
                alerts.add(new String[] {h.window(), names[i], i == 0 ? h.subject() + " failed " + h.events() + " times in five minutes" : i == 1 ? h.events() + " failed logins from " + h.subject() + " on many accounts in twenty minutes" : i == 2 ? "logins from " + h.subject() + ": " + h.events() + " in the hour, failures and a success" : h.subject(), h.truePositive() ? "" : "   <- FALSE POSITIVE"});
            }
        }
        line(out, "  " + os.monitors() + " monitors are saved in the cluster, each with a schedule of one minute; here each is run once with dryrun, so that nothing is sent and the result is the same every time");
        List<Event> fromIndex = os.events("audit");
        List<Alert> travel = new ImpossibleTravelRule(900, 500, Set.of(Scenario.VPN_EGRESS)).evaluate(fromIndex);
        List<Hit> travelHits = new ArrayList<>();
        for (Alert a : travel) {
            Set<String> at = new TreeSet<>();
            a.evidence().stream().map(Event::attackId).filter(x -> x != null).forEach(at::add);
            travelHits.add(new Hit(a.subject(), a.time().toString().substring(11, 16), 2, at));
            alerts.add(new String[] {a.time().toString().substring(11, 16), "impossible-travel", a.summary(), at.isEmpty() ? "   <- FALSE POSITIVE" : ""});
        }
        row(out, "impossible-travel (code)", travelHits);
        all.addAll(travelHits);
        long tp = all.stream().filter(Hit::truePositive).count();
        line(out, "together: " + all.size() + " alerts for the day, " + tp + " true, " + (all.size() - tp) + " false; attacks noticed: " + attacks(all) + " of [A, B, C, D]");
        line(out, "  impossible travel compares each login with the same person's last one: a query cannot say \"the previous one\", so the events are read back from OpenSearch in order and a few lines of code decide");
        line(out, "the alerts, in time order:");
        alerts.sort(Comparator.comparing((String[] a) -> a[0]).thenComparing(a -> a[1]));
        alerts.forEach(a -> line(out, String.format("  %s %-18s %s%s", a[0], a[1], a[2], a[3])));
    }

    private static void act5(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "FIVE. The bill.");
        os.bulk("dynamic", Scenario.day().subList(0, 50));
        OpenSearch.Answer bad = os.search("dynamic", Map.of("size", 0, "aggs", Map.of("by_user", Map.of("terms", Map.of("field", "user")))));
        line(out, "  the same events loaded with no mapping, grouped by user: " + bad.status() + ", " + bad.json().get("error").get("root_cause").get(0).get("reason").asString().replaceAll("\\[[^\\]]*\\]", "[...]").replaceAll("Alternatively.*", "..."));
        OpenSearch.Answer good = os.search("dynamic", Map.of("size", 0, "aggs", Map.of("by_user", Map.of("terms", Map.of("field", "user.keyword", "size", 3)))));
        line(out, "  grouped by user.keyword, which dynamic mapping adds next to the text: " + good.status() + ", " + good.json().get("aggregations").get("by_user").get("buckets").size() + " groups  <- A MAPPING DECIDES WHAT A RULE CAN SAY");
        os.createAuditIndex("straddle");
        List<Event> burst = new ArrayList<>();
        for (int s = 0; s < 30; s++) {
            burst.add(Event.login(Scenario.at(13, 28, 0).plusSeconds(s * 8L), Scenario.user(77), "198.51.100.77", City.SINGAPORE, false, "B"));
        }
        List<Hit> bucketed = Detections.run(os, "straddle", Detections.bruteForce(20));
        List<Alert> sliding = new BruteForceRuleForStraddle().evaluate(burst);
        line(out, "  thirty failures in four minutes that cross a five-minute boundary at 13:30, rule 'at least 20': fixed buckets " + bucketed.size() + " alerts, a sliding window " + sliding.size() + "  <- THE ATTACK FALLS BETWEEN TWO BUCKETS");
        line(out, "  the monitors run on a schedule, so an attack is noticed up to a minute and a bucket later, and a schedule that stops, or a query that is changed in the console, leaves no trace in the code");
        line(out, "  keep the queries as files under version control and load them with the API, test each with a known bad day, and treat the thresholds as tuned for this one Monday");
    }

    /** A sliding five-minute window, as the plain project has, for comparison with fixed buckets. */
    static final class BruteForceRuleForStraddle implements Rule {

        @Override
        public String name() {
            return "brute-force(20)";
        }

        @Override
        public List<Alert> evaluate(List<Event> log) {
            java.util.Deque<Event> q = new java.util.ArrayDeque<>();
            List<Alert> alerts = new ArrayList<>();
            boolean firing = false;
            for (Event e : log) {
                while (!q.isEmpty() && Duration.between(q.peekFirst().time(), e.time()).compareTo(Duration.ofMinutes(5)) > 0) {
                    q.removeFirst();
                }
                q.addLast(e);
                if (q.size() >= 20 && !firing) {
                    firing = true;
                    alerts.add(new Alert(name(), e.user(), e.time(), q.size() + " failed logins", List.copyOf(q)));
                }
            }
            return alerts;
        }
    }
}
