package com.jk.explore.security.lokilogs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.databind.json.JsonMapper;

public class LokiLogsDemo {

    private static final JsonMapper JSON = JsonMapper.builder().build();

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

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static String field(Loki.Line line, String name) {
        return JSON.readTree(line.text()).get(name).asString();
    }

    static void ship(Loki loki, List<Event> events, Instant start) throws Exception {
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            int status = loki.push(Map.of("app", "bank", "event_type", e.eventType()), start.plusMillis(i * 100L), e.line()).status();
            if (status != 204) {
                throw new IllegalStateException("Loki answered " + status);
            }
        }
    }

    private static void act1(StringBuilder out, Lab lab, Loki bank) throws Exception {
        line(out, "ONE. Shipping the events.");
        List<Event> events = Event.morning();
        ship(bank, events, Instant.now().minusSeconds(60));
        line(out, "  the morning's " + events.size() + " events are pushed as JSON lines to a real Loki, each answered with 204, with two labels: app=bank and event_type");
        line(out, "  label names Loki now knows (service_name it adds itself): " + bank.labels() + "; streams: " + bank.streams("{app=\"bank\"}") + ", one for each of the " + new TreeSet<>(events.stream().map(Event::eventType).toList()).size() + " event types");
        Loki nobody = new Loki(lab.lokiUrl(), null);
        Loki.Answer refused = nobody.push(Map.of("app", "bank"), Instant.now(), "no tenant");
        line(out, "  a push with no tenant header: " + refused.status() + " " + refused.body().strip() + "  <- EVERY REQUEST NAMES WHOSE LOGS IT IS ABOUT");
    }

    private static void act2(StringBuilder out, Loki bank) throws Exception {
        line(out, "TWO. Asking questions in LogQL.");
        List<Loki.Line> mentions = bank.lines("{app=\"bank\"} |= \"ANA-1\"");
        line(out, "  which events mention ANA-1, by a text filter: " + mentions.size());
        List<Loki.Line> transfer = bank.lines("{app=\"bank\", event_type=\"money-movement\"} | json | action=\"transfer\" | amount >= 500");
        Loki.Line t = transfer.get(0);
        line(out, "  who moved 500 or more: " + field(t, "actor") + " from " + field(t, "source_ip") + ", " + field(t, "resource") + ", " + field(t, "outcome") + ", request " + field(t, "request_id"));
        List<Loki.Line> sameAddress = bank.lines("{app=\"bank\"} | json | source_ip=\"" + field(t, "source_ip") + "\"");
        List<String> story = new ArrayList<>();
        sameAddress.forEach(l -> story.add(field(l, "event_type") + " " + field(l, "outcome")));
        line(out, "  everything from that address, in time order: " + story + "  <- SOMEONE WITH ANA'S PASSWORD, NOT ANA");
        line(out, "  failures by address, counted by Loki: " + bank.metric("sum by (source_ip) (count_over_time({app=\"bank\"} | json | outcome=\"failure\" [1h]))", "source_ip"));
        line(out, "  the question that the old logger could not answer is one line of LogQL");
    }

    private static void act3(StringBuilder out, Loki bank) throws Exception {
        line(out, "THREE. Labels and content.");
        Instant now = Instant.now();
        for (int i = 0; i < 150; i++) {
            Event e = new Event("req-1" + String.format("%03d", i), "login", "user" + i, "login", "session", "203.0.113.5", "success", null);
            bank.push(Map.of("app", "bank-labelled", "event_type", "login", "actor", "user" + i), now.minusSeconds(30), e.line());
            bank.push(Map.of("app", "bank-content", "event_type", "login"), now.minusSeconds(30).plusNanos(i), e.line());
        }
        line(out, "  150 more logins, once with the user as a label and once as part of the line: streams " + bank.streams("{app=\"bank-labelled\"}") + " against " + bank.streams("{app=\"bank-content\"}"));
        line(out, "  asking for one user: " + bank.lines("{app=\"bank-labelled\", actor=\"user7\"}").size() + " line by the label, " + bank.lines("{app=\"bank-content\"} | json | actor=\"user7\"").size() + " line by the content");
        line(out, "  every label value makes a stream of its own and an entry in the index, so labels are for the few things you always select by (the application, the kind of event) and the rest stays in the line");
    }

    private static void act4(StringBuilder out, Loki bank) throws Exception {
        line(out, "FOUR. Alerting.");
        line(out, "  two rules are loaded for the tenant, evaluated every 2 seconds: " + bank.ruleStates());
        line(out, "  alerts firing now, with ana's two failed logins from 198.51.100.66 in the logs: " + bank.firingAlerts());
        Instant now = Instant.now();
        for (int i = 0; i < 4; i++) {
            Event e = new Event("req-2" + i, "login", "ben", "login", "session", "203.0.113.9", "failure", null);
            bank.push(Map.of("app", "bank", "event_type", "login"), now.plusMillis(i), e.line());
        }
        List<String> firing = List.of();
        for (int i = 0; i < 60 && firing.isEmpty(); i++) {
            Thread.sleep(1000);
            firing = bank.firingAlerts();
        }
        line(out, "  four failed logins from 203.0.113.9 are shipped; a moment later: " + firing);
        line(out, "  the alert's text: " + bank.summaryOfFirstAlert());
        line(out, "  the rule with a typo in the field name, outcom: " + bank.ruleStates().get("PasswordGuessingTypo") + "  <- IT RUNS WITHOUT A COMPLAINT AND NEVER FIRES");
        line(out, "  a rule is code: test it with a known bad event, on a schedule, and expect it to fire");
    }

    private static void act5(StringBuilder out, Lab lab, Loki bank) throws Exception {
        line(out, "FIVE. The bill.");
        Loki shop = new Loki(lab.lokiUrl(), "shop");
        line(out, "  another tenant, shop, asking for the bank's lines: " + shop.lines("{app=\"bank\"}").size() + " of " + bank.lines("{app=\"bank\"}").size() + "; its labels: " + shop.labels());
        Loki.Answer old = bank.push(Map.of("app", "bank"), Instant.now().minusSeconds(10L * 86400), "{\"event_type\":\"login\"}");
        line(out, "  an event stamped ten days ago: " + old.status() + " " + old.body().strip().replaceAll("'[^']*'", "'...'").replaceAll("[0-9T:.-]+Z", "...").replaceAll("\\d+ ?[hmsd.]+[0-9hmsd]*", "..."));
        Loki.Answer delete = bank.deleteLines("{app=\"bank\"}");
        line(out, "  asking Loki to delete the bank's lines through its API: " + delete.status() + ", the route is not even there until retention and deletion are configured; changing a line after it was sent is not offered either");
        Grafana grafana = new Grafana(lab.grafanaUrl());
        String uid = grafana.addLoki("bank", "bank");
        String shopUid = grafana.addLoki("shop", "shop");
        line(out, "  Grafana, with a data source that sends the tenant bank: connection " + grafana.health(uid) + ", the same question gives " + grafana.count(uid, "{app=\"bank\"} | json | outcome=\"failure\"") + " failures against Loki's " + bank.lines("{app=\"bank\"} | json | outcome=\"failure\"").size()
                + "; through the shop's data source: " + grafana.count(shopUid, "{app=\"bank\"}"));
        line(out, "  whoever can use a data source sees that tenant's logs, so access to Grafana is access to the logs; Loki's tenants separate the data and do not log anyone in");
        line(out, "  Loki stores what it is sent and does not prove that it was not changed before it was sent; the hash chain of the plain project is still written by the sender");
        line(out, "  retention, the size of the index and what the logs may contain (see secrets-in-errors-and-logs) remain the team's to decide");
    }
}
