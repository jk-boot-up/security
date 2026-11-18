package com.jk.explore.security.lokilogs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ships the online bank's security events to a real Loki, asks LogQL questions, compares labels with content, raises an alert, and
 * shows what is left to the team. Each act prints labelled sections: the input, then the result, with derived values worked out.
 */
public class LokiLogsDemo {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Ana's failures and Ben's: the question asked by the metric query and by the alert rule. */
    static final String FAILURES = "{app=\"bank\"} | json | outcome=\"failure\"";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            Loki bank = new Loki(lab.lokiUrl(), "bank");
            act1(out, lab, bank);
            act2(out, bank);
            act3(out, bank);
            act4(out, bank);
            act5(out, lab, bank);
        }
        return out.toString();
    }

    private static void act(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void section(StringBuilder out, String title) {
        out.append("  ").append(title).append(":\n");
    }

    private static void row(StringBuilder out, String text) {
        out.append("    ").append(text).append('\n');
    }

    private static void fact(StringBuilder out, int width, String key, Object value) {
        row(out, pad(key, width) + value);
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(2, width - text.length()));
    }

    static String field(Loki.Line line, String name) {
        return JSON.readTree(line.text()).get(name).asString();
    }

    static Map<String, String> labelsOf(Event e) {
        return Map.of("app", "bank", "event_type", e.eventType());
    }

    static void ship(Loki loki, List<Event> events, Instant start) throws Exception {
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            int status = loki.push(labelsOf(e), start.plusMillis(i * 100L), e.line()).status();
            if (status != 204) {
                throw new IllegalStateException("Loki answered " + status);
            }
        }
    }

    private static void act1(StringBuilder out, Lab lab, Loki bank) throws Exception {
        act(out, "ONE. Shipping the events.");
        List<Event> events = Event.morning();
        section(out, "ONE EVENT AS A JSON LINE (line 6 of " + events.size() + ", the transfer)");
        for (String piece : Streams.wrap(events.get(5).line(), 58)) {
            row(out, piece);
        }
        section(out, "THE PUSH (each line, with the tenant header bank)");
        fact(out, 10, "header", "X-Scope-OrgID: bank");
        fact(out, 10, "labels", "app=bank, event_type=<the event's type>");
        ship(bank, events, Instant.now().minusSeconds(60));
        fact(out, 10, "answers", events.size() + " x 204 (stored, nothing to return)");
        section(out, "WHAT LOKI HOLDS NOW (a stream per label set)");
        Map<String, Integer> perType = new LinkedHashMap<>();
        events.forEach(e -> perType.merge(e.eventType(), 1, Integer::sum));
        perType.forEach((type, n) -> row(out, pad("{app=\"bank\", event_type=\"" + type + "\"}", 48) + n + " line" + (n == 1 ? "" : "s")));
        fact(out, 13, "streams", bank.streams("{app=\"bank\"}") + " (worked out: "
                + Streams.count(events.stream().map(LokiLogsDemo::labelsOf).toList()) + " distinct label sets)");
        fact(out, 13, "label names", String.join(", ", bank.labels()) + " (Loki adds the last)");
        Loki nobody = new Loki(lab.lokiUrl(), null);
        Loki.Answer refused = nobody.push(Map.of("app", "bank"), Instant.now(), "no tenant");
        section(out, "A PUSH WITH NO TENANT HEADER");
        fact(out, 10, "answer", refused.status() + " " + refused.body().strip() + "  <- WHOSE LOGS? IT MUST SAY");
    }

    private static void act2(StringBuilder out, Loki bank) throws Exception {
        act(out, "TWO. Asking questions in LogQL.");
        String text = "{app=\"bank\"} |= \"ANA-1\"";
        section(out, "QUERY 1, A TEXT FILTER (which lines mention ANA-1?)");
        row(out, text);
        List<Loki.Line> mentions = bank.lines(text);
        section(out, "ITS ANSWER (" + mentions.size() + " lines)");
        for (Loki.Line l : mentions) {
            row(out, pad(field(l, "request_id"), 10) + pad(field(l, "action"), 14) + field(l, "resource"));
        }
        String byFields = "{app=\"bank\", event_type=\"money-movement\"} | json | action=\"transfer\" | amount >= 500";
        section(out, "QUERY 2, BY FIELDS (who moved 500 or more?)");
        row(out, "{app=\"bank\", event_type=\"money-movement\"}");
        row(out, "  | json | action=\"transfer\" | amount >= 500");
        Loki.Line t = bank.lines(byFields).get(0);
        section(out, "ITS ANSWER");
        for (String name : List.of("actor", "source_ip", "resource", "outcome", "request_id")) {
            fact(out, 12, name, field(t, name));
        }
        String fromThere = "{app=\"bank\"} | json | source_ip=\"" + field(t, "source_ip") + "\"";
        section(out, "QUERY 3, EVERYTHING FROM THAT ADDRESS");
        row(out, fromThere);
        section(out, "ITS ANSWER (in time order)");
        for (Loki.Line l : bank.lines(fromThere)) {
            row(out, pad(field(l, "request_id"), 10) + pad(field(l, "actor"), 6) + pad(field(l, "event_type"), 16) + field(l, "outcome"));
        }
        row(out, "2 failures, 1 success, the transfer  <- ANA'S PASSWORD, NOT ANA");
        section(out, "QUERY 4, A METRIC (failures by address, counted by Loki)");
        row(out, "sum by (source_ip) (count_over_time(");
        row(out, "  " + FAILURES + " [1h]))");
        section(out, "ITS ANSWER");
        bank.metric("sum by (source_ip) (count_over_time(" + FAILURES + " [1h]))", "source_ip")
                .forEach((ip, n) -> fact(out, 15, ip, Math.round(n) + " failures"));
    }

    private static void act3(StringBuilder out, Loki bank) throws Exception {
        act(out, "THREE. Labels and content.");
        Instant now = Instant.now();
        int users = 150;
        List<Map<String, String>> labelled = new ArrayList<>();
        List<Map<String, String>> content = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            Event e = new Event("req-1" + String.format("%03d", i), "login", "user" + i, "login", "session", "203.0.113.5", "success", null);
            Map<String, String> asLabel = Map.of("app", "bank-labelled", "event_type", "login", "actor", "user" + i);
            Map<String, String> inLine = Map.of("app", "bank-content", "event_type", "login");
            bank.push(asLabel, now.minusSeconds(30), e.line());
            bank.push(inLine, now.minusSeconds(30).plusNanos(i), e.line());
            labelled.add(asLabel);
            content.add(inLine);
        }
        section(out, "THE INPUT (" + users + " more logins, sent twice)");
        fact(out, 10, "labelled", "app=bank-labelled, event_type=login,");
        fact(out, 10, "", "actor=user0 ... user" + (users - 1));
        fact(out, 10, "content", "app=bank-content, event_type=login;");
        fact(out, 10, "", "the actor only inside the JSON line");
        section(out, "THE STREAMS (worked out, and counted by Loki)");
        fact(out, 10, "labelled", "1 app x 1 type x " + users + " actors = " + Streams.count(labelled)
                + "; Loki: " + bank.streams("{app=\"bank-labelled\"}"));
        fact(out, 10, "content", "1 app x 1 type = " + Streams.count(content) + "; Loki: " + bank.streams("{app=\"bank-content\"}"));
        String byLabel = "{app=\"bank-labelled\", actor=\"user7\"}";
        String byContent = "{app=\"bank-content\"} | json | actor=\"user7\"";
        section(out, "ASKING FOR ONE USER, user7");
        row(out, byLabel);
        row(out, "  -> " + bank.lines(byLabel).size() + " line, from 1 of " + users + " streams");
        row(out, byContent);
        row(out, "  -> " + bank.lines(byContent).size() + " line, the 1 stream read and filtered");
        section(out, "THE RULE OF THUMB");
        row(out, "a label: the few things you always select by");
        row(out, "the line: everything else (users, addresses, amounts)");
    }

    private static void act4(StringBuilder out, Loki bank) throws Exception {
        act(out, "FOUR. Alerting.");
        String count = "sum by (source_ip) (count_over_time(" + FAILURES + " [5m]))";
        section(out, "THE RULE PasswordGuessing (checked every 2 seconds)");
        row(out, "sum by (source_ip) (count_over_time(");
        row(out, "  " + FAILURES + " [5m])) >= 3");
        Map<String, String> states = bank.ruleStates();
        section(out, "BEFORE (ana's 2 failures are in the logs)");
        states.forEach((name, state) -> fact(out, 22, name, state));
        bank.metric(count, "source_ip").forEach((ip, n) -> fact(out, 22, ip, Math.round(n) + " >= 3? " + (n >= 3 ? "yes" : "no")));
        fact(out, 22, "firing", bank.firingAlerts().isEmpty() ? "none" : String.join(", ", bank.firingAlerts()));
        Instant now = Instant.now();
        int tries = 4;
        for (int i = 0; i < tries; i++) {
            Event e = new Event("req-2" + i, "login", "ben", "login", "session", "203.0.113.9", "failure", null);
            bank.push(Map.of("app", "bank", "event_type", "login"), now.plusMillis(i), e.line());
        }
        List<String> firing = List.of();
        for (int i = 0; i < 60 && firing.isEmpty(); i++) {
            Thread.sleep(1000);
            firing = bank.firingAlerts();
        }
        section(out, "THE INPUT (" + tries + " failed logins as ben, from 203.0.113.9)");
        row(out, "shipped like every other event");
        section(out, "AFTER (a moment later)");
        bank.metric(count, "source_ip").forEach((ip, n) -> fact(out, 15, ip, Math.round(n) + " >= 3? " + (n >= 3 ? "yes" : "no")));
        fact(out, 15, "firing", String.join(", ", firing));
        row(out, "its text");
        row(out, "  " + bank.summaryOfFirstAlert());
        section(out, "THE RULE PasswordGuessingTypo (outcom, not outcome)");
        String typo = "sum by (source_ip) (count_over_time({app=\"bank\"} | json | outcom=\"failure\" [5m]))";
        fact(out, 8, "counts", bank.metric(typo, "source_ip").isEmpty() ? "none: no line has a field outcom" : "some");
        fact(out, 8, "state", bank.ruleStates().get("PasswordGuessingTypo") + "  <- NO ERROR, AND IT NEVER FIRES");
        row(out, "so test a rule with a known bad event, and expect it to fire");
    }

    private static void act5(StringBuilder out, Lab lab, Loki bank) throws Exception {
        act(out, "FIVE. The bill.");
        Loki shop = new Loki(lab.lokiUrl(), "shop");
        String all = "{app=\"bank\"}";
        section(out, "ANOTHER TENANT, shop, ASKS FOR " + all);
        fact(out, 8, "lines", shop.lines(all).size() + " (the tenant bank sees " + bank.lines(all).size() + ")");
        fact(out, 8, "labels", shop.labels().isEmpty() ? "none" : String.join(", ", shop.labels()));
        Loki.Answer old = bank.push(Map.of("app", "bank"), Instant.now().minusSeconds(10L * 86400), "{\"event_type\":\"login\"}");
        section(out, "AN EVENT STAMPED TEN DAYS AGO");
        java.util.regex.Matcher limit = java.util.regex.Pattern.compile("reject_old_samples_max_age: (\\d+)h").matcher(Lab.LOKI_CONFIG);
        limit.find();
        int hours = Integer.parseInt(limit.group(1));
        fact(out, 8, "limit", "reject_old_samples_max_age " + hours + "h = " + hours + " / 24 = " + hours / 24 + " days");
        fact(out, 8, "event", "10 days old > " + hours / 24 + " days");
        fact(out, 8, "answer", old.status() + (old.body().contains("too old") ? " timestamp too old  <- REFUSED" : " " + old.body().strip()));
        Loki.Answer delete = bank.deleteLines(all);
        section(out, "ASKING LOKI TO DELETE THE BANK'S LINES");
        fact(out, 8, "answer", delete.status() + ": no such route until deletion is configured");
        row(out, "and no route at all changes a line once it is stored");
        Grafana grafana = new Grafana(lab.grafanaUrl());
        String uid = grafana.addLoki("bank", "bank");
        String shopUid = grafana.addLoki("shop", "shop");
        section(out, "GRAFANA, ONE DATA SOURCE PER TENANT HEADER");
        fact(out, 7, "bank", "connection " + grafana.health(uid) + ", failures " + grafana.count(uid, FAILURES)
                + " (Loki itself: " + bank.lines(FAILURES).size() + ")");
        fact(out, 7, "shop", "connection " + grafana.health(shopUid) + ", bank lines " + grafana.count(shopUid, all));
        section(out, "STILL THE TEAM'S JOB");
        row(out, "who may use a data source: that is who reads the logs");
        row(out, "proof a line was not changed before it was sent");
        row(out, "retention, index size, and no secrets in the lines");
    }
}
