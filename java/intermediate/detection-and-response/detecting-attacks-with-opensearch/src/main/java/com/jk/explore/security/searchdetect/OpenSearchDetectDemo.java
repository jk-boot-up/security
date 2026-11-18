package com.jk.explore.security.searchdetect;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/**
 * Runs the plain project's detections as OpenSearch queries and alerting monitors, in five numbered acts, each with its input and its result in separate, labelled
 * sections. The day of logs is synthetic with a fixed seed and a fixed date, the image is pinned, the monitors run once with dryrun and nothing is time-ranged
 * against the clock, so the output is the same on every run. Ports and container names are never printed.
 */
public class OpenSearchDetectDemo {

    static final int TUNED_BRUTE_FORCE = 10;

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

    private static void line(StringBuilder out, String format, Object... args) {
        out.append(String.format(format, args)).append('\n');
    }

    private static Set<String> attacks(List<Hit> hits) {
        Set<String> out = new TreeSet<>();
        hits.forEach(h -> out.addAll(h.attacks()));
        return out;
    }

    private static void tableHeader(StringBuilder out) {
        line(out, "    %-27s %6s %5s %5s  %s", "rule", "alerts", "true", "false", "attacks");
    }

    private static void row(StringBuilder out, String name, List<Hit> hits) {
        long tp = hits.stream().filter(Hit::truePositive).count();
        line(out, "    %-27s %6d %5d %5d  %s", name, hits.size(), tp, hits.size() - tp, String.join(",", attacks(hits)));
    }

    private static void act1(StringBuilder out, OpenSearch os, List<Event> day) throws Exception {
        line(out, "ONE. One Monday of logs, in OpenSearch.");
        line(out, "  THE INPUT (the plain project's Monday, sent with the bulk API):");
        line(out, "    events        %d", day.size());
        line(out, "  THE MAPPING (set before the first event, read back):");
        os.mapping("audit").forEach((type, fields) -> line(out, "    %-9s %s", type, String.join(", ", fields)));
        line(out, "  ONE EVENT AS A DOCUMENT (u042's first failed login, read back):");
        JsonNode doc = os.search("audit", Map.of("size", 1, "sort", List.of(Map.of("time", "asc")), "query",
                Map.of("bool", Map.of("filter", List.of(Map.of("term", Map.of("user", "u042")), Map.of("term", Map.of("success", false))))))).json()
                .get("hits").get("hits").get(0).get("_source");
        for (String field : List.of("time", "type", "user", "ip", "city", "success", "attack")) {
            line(out, "    %-8s %s%s", field, doc.get(field).asString(), field.equals("attack") ? "  <- FOR SCORING ONLY" : "");
        }
        line(out, "  THE RESULT (counted by OpenSearch, with _count):");
        line(out, "    logins        %d", os.count("audit", Map.of("term", Map.of("type", "login"))));
        line(out, "    role grants   %d", os.count("audit", Map.of("term", Map.of("type", "role-grant"))));
        line(out, "    with attack   %d, of 4 attacks; no rule reads the field", os.count("audit", Map.of("exists", Map.of("field", "attack"))));
    }

    private static void act2(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "TWO. A first version of each rule, as a query.");
        line(out, "  THE QUERY (brute-force(3), its four parts):");
        QueryParts.of(Detections.bruteForce(3)).forEach((part, value) -> line(out, "    %-10s %s", part, value));
        List<Hit> buckets = new ArrayList<>(Detections.run(os, "audit", Detections.bruteForce(3)));
        buckets.sort(Comparator.comparing(Hit::events).reversed().thenComparing(Hit::subject));
        line(out, "  THE BUCKETS IT RETURNS (failures, one account, 5 minutes):");
        for (Hit h : buckets) {
            line(out, "    %s  %s  %2d  %s", h.subject(), h.window(), h.events(), h.truePositive() ? "attack " + String.join(",", h.attacks()) : "<- FALSE");
        }
        line(out, "  THE RESULT (every first rule, scored):");
        tableHeader(out);
        List<Hit> all = new ArrayList<>();
        for (Object[] r : new Object[][] {{"brute-force(3)", Detections.bruteForce(3)}, {"spraying(8)", Detections.spraying(8)},
            {"privilege-change", Detections.privilegeChange()}, {"success-after-failures(1)", Detections.successAfterFailures(1)}}) {
            @SuppressWarnings("unchecked")
            List<Hit> hits = Detections.run(os, "audit", (Map<String, Object>) r[1]);
            row(out, (String) r[0], hits);
            all.addAll(hits);
        }
        row(out, "together", all);
        line(out, "  WHY success-after-failures(1) IS NOISY:");
        QueryParts.of(Detections.successAfterFailures(1)).forEach((part, value) -> line(out, "    %-10s %s", part, value));
        line(out, "    a query cannot say \"then\": a typo and a success from one");
        line(out, "    address in the same hour is enough  <- MOSTLY FALSE");
    }

    private static void act3(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "THREE. Tuning one threshold.");
        List<Hit> every = Detections.run(os, "audit", Detections.bruteForce(1));
        Hit normal = every.stream().filter(h -> !h.truePositive()).max(Comparator.comparing(Hit::events)).orElseThrow();
        Hit attack = every.stream().filter(Hit::truePositive).max(Comparator.comparing(Hit::events)).orElseThrow();
        long sameAsNormal = every.stream().filter(h -> !h.truePositive() && h.events() == normal.events()).count();
        line(out, "  THE BUCKETS (min_doc_count 1: every account's 5-minute buckets):");
        line(out, "    buckets            %d", every.size());
        line(out, "    largest normal     %d failures (%d buckets, e.g. %s at %s)", normal.events(), sameAsNormal, normal.subject(), normal.window());
        line(out, "    largest attack     %d failures (%s at %s)", attack.events(), attack.subject(), attack.window());
        line(out, "  THE SAFE RANGE (worked out):");
        line(out, "    more than %d, at most %d: min_doc_count %d to %d; chosen: %d", normal.events(), attack.events(), normal.events() + 1, attack.events(),
                TUNED_BRUTE_FORCE);
        line(out, "  THE SWEEP (one number changed in the query):");
        line(out, "    %13s %7s %5s %6s  %s", "min_doc_count", "alerts", "true", "false", "attack B");
        for (int t : new int[] {3, 5, 10, 25, 40}) {
            List<Hit> hits = Detections.run(os, "audit", Detections.bruteForce(t));
            long tp = hits.stream().filter(Hit::truePositive).count();
            line(out, "    %13d %7d %5d %6d  %s", t, hits.size(), tp, hits.size() - tp, attacks(hits).contains("B") ? "noticed" : "MISSED");
        }
    }

    private static void act4(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "FOUR. The tuned set as monitors of the alerting plugin.");
        Map<String, Object> shown = Monitors.bruteForce(TUNED_BRUTE_FORCE);
        line(out, "  THE MONITOR (%s, as saved in the cluster):", shown.get("name"));
        line(out, "    type      %s", shown.get("monitor_type"));
        line(out, "    schedule  every %s", Monitors.schedule(shown));
        line(out, "    input     the brute-force query, as a composite aggregation");
        line(out, "    trigger   %s", Monitors.condition(shown));
        String[] names = {"brute-force(10)", "spraying(8)", "success-after-failures(20)", "privilege-change"};
        List<Map<String, Object>> monitors = List.of(shown, Monitors.spraying(8), Monitors.successAfterFailures(20), Monitors.privilegeChange());
        List<String[]> alerts = new ArrayList<>();
        List<Hit> all = new ArrayList<>();
        line(out, "  THE RUN (each monitor once, with dryrun: nothing is sent):");
        tableHeader(out);
        for (int i = 0; i < monitors.size(); i++) {
            JsonNode run = os.execute(os.createMonitor(monitors.get(i)));
            List<Hit> hits;
            if (i < 3) {
                hits = Monitors.triggered(run);
            } else {
                hits = run.get("trigger_results").propertyStream().findFirst().orElseThrow().getValue().get("triggered").asBoolean() ? Detections.run(os, "audit", Detections.privilegeChange()) : List.of();
            }
            row(out, names[i], hits);
            all.addAll(hits);
            for (Hit h : hits) {
                alerts.add(new String[] {h.window(), names[i].replaceAll("\\(.*", ""), h.subject().split(" ")[0], h.truePositive() ? String.join(",", h.attacks()) : "<- FALSE"});
            }
        }
        List<Event> fromIndex = os.events("audit");
        List<Hit> travelHits = new ArrayList<>();
        for (Alert a : new ImpossibleTravelRule(900, 500, Set.of(Scenario.VPN_EGRESS)).evaluate(fromIndex)) {
            Set<String> at = new TreeSet<>();
            a.evidence().stream().map(Event::attackId).filter(x -> x != null).forEach(at::add);
            Hit h = new Hit(a.subject(), a.time().toString().substring(11, 16), 2, at);
            travelHits.add(h);
            alerts.add(new String[] {h.window(), "impossible-travel", h.subject(), h.truePositive() ? String.join(",", at) : "<- FALSE"});
        }
        row(out, "impossible-travel (code)", travelHits);
        all.addAll(travelHits);
        row(out, "together", all);
        line(out, "  THE ALERTS, IN TIME ORDER (a monitor gives its bucket's start):");
        alerts.sort(Comparator.comparing((String[] a) -> a[0]).thenComparing(a -> a[1]));
        alerts.forEach(a -> line(out, "    %s  %-22s  %-12s  %s", a[0], a[1], a[2], a[3]));
        line(out, "  WHY IMPOSSIBLE TRAVEL IS CODE:");
        line(out, "    a query cannot say \"this person's previous login\", so the");
        line(out, "    %d events are read back in time order (search_after)", fromIndex.size());
        line(out, "    and ImpossibleTravelRule decides: %d alerts", travelHits.size());
        line(out, "    %d monitors are saved, each on a 1-minute schedule", os.monitors());
    }

    private static void act5(StringBuilder out, OpenSearch os) throws Exception {
        line(out, "FIVE. The bill.");
        os.bulk("dynamic", Scenario.day().subList(0, 50));
        line(out, "  THE INPUT (50 events, no mapping, grouped by user):");
        OpenSearch.Answer bad = os.search("dynamic", Map.of("size", 0, "aggs", Map.of("by_user", Map.of("terms", Map.of("field", "user")))));
        String reason = bad.json().get("error").get("root_cause").get(0).get("reason").asString();
        line(out, "    status   %d", bad.status());
        line(out, "    reason   %s", reason.substring(0, reason.indexOf(" for operations")));
        line(out, "             for operations that require per-document field data");
        OpenSearch.Answer good = os.search("dynamic", Map.of("size", 0, "aggs", Map.of("by_user", Map.of("terms", Map.of("field", "user.keyword", "size", 3)))));
        line(out, "  THE SAME, BY user.keyword (which dynamic mapping adds):");
        line(out, "    status   %d, %d groups  <- THE MAPPING DECIDES", good.status(), good.json().get("aggregations").get("by_user").get("buckets").size());

        os.createAuditIndex("straddle");
        List<Event> burst = new ArrayList<>();
        for (int s = 0; s < 30; s++) {
            burst.add(Event.login(Scenario.at(13, 28, 0).plusSeconds(s * 8L), Scenario.user(77), "198.51.100.77", City.SINGAPORE, false, "B"));
        }
        os.bulk("straddle", burst);
        line(out, "  THE INPUT (30 failures for %s, one every 8 seconds):", burst.getFirst().user());
        line(out, "    from %s to %s, crossing 13:30", hms(burst.getFirst().time()), hms(burst.getLast().time()));
        line(out, "  FIXED BUCKETS (5 minutes each, the rule: at least 20):");
        List<Hit> perBucket = Detections.run(os, "straddle", Detections.bruteForce(1));
        Map<Instant, Long> byHand = Buckets.fixed(burst, Duration.ofMinutes(5));
        for (Hit h : perBucket) {
            line(out, "    bucket %s-%s  %d failures, under 20", h.window(), hm(Instant.parse("2026-03-02T" + h.window() + ":00Z").plusSeconds(300)), h.events());
        }
        line(out, "    worked out by hand: %s", byHand.values());
        line(out, "    alerts   %d  <- THE ATTACK FALLS BETWEEN TWO BUCKETS", Detections.run(os, "straddle", Detections.bruteForce(20)).size());
        List<Alert> sliding = new BruteForceRuleForStraddle().evaluate(burst);
        line(out, "  A SLIDING WINDOW (the plain project's rule, at least 20):");
        line(out, "    alerts   %d, at %s: the 20th failure, %s + 19 x 8 s", sliding.size(), hms(sliding.getFirst().time()), hms(burst.getFirst().time()));
        line(out, "  THE SCHEDULE:");
        line(out, "    a monitor runs every minute on fixed buckets, so it notices");
        line(out, "    up to a minute and a bucket late; a stopped schedule or a");
        line(out, "    query edited in the console leaves no trace in the code");
    }

    private static String hms(Instant t) {
        return t.toString().substring(11, 19);
    }

    private static String hm(Instant t) {
        return t.toString().substring(11, 16);
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
