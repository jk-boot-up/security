package com.jk.explore.security.siembasics;

import com.jk.explore.security.siembasics.Correlator.Alert;
import com.jk.explore.security.siembasics.Parsers.Parsed;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class SiemBasicsDemo {

    static final long SEED = 20261023L;
    static final String ATTACKER = "203.0.113.5";
    static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    public static void main(String[] args) {
        System.out.print(run());
    }

    static List<Event> all(Parsed web, Parsed db, Parsed host) {
        List<Event> events = new ArrayList<>(web.events());
        events.addAll(db.events());
        events.addAll(host.events());
        events.sort(java.util.Comparator.comparing(Event::at));
        return events;
    }

    static String clock(Instant at) {
        return CLOCK.format(at);
    }

    /** A long log line cut at spaces or after commas, each piece at most width characters where it can be. */
    static List<String> wrap(String line, int width) {
        List<String> out = new ArrayList<>();
        String rest = line;
        while (rest.length() > width) {
            int cut = Math.max(rest.lastIndexOf(' ', width), rest.lastIndexOf(',', width - 1) + 1);
            if (cut <= 0) {
                cut = width;
            }
            out.add(rest.substring(0, cut).stripTrailing());
            rest = rest.substring(cut).stripLeading();
        }
        out.add(rest);
        return out;
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Sources.Lines lines = Sources.day(SEED);
        Parsed web = Parsers.web(lines.web());
        Parsed db = Parsers.db(lines.db(), true);
        Parsed host = Parsers.host(lines.host(), 2026);
        List<Event> events = all(web, db, host);

        // ONE: one line of each source, and the one shape it becomes
        out.append("ONE. Three sources, three formats, one shape.\n");
        String webLine = lines.web().stream().filter(l -> l.contains("\"alice\"") && l.contains("\"ok\":true")).findFirst().orElseThrow();
        String dbLine = lines.db().stream().filter(l -> l.contains("actor=alice ")).findFirst().orElseThrow();
        String hostLine = lines.host().stream().filter(l -> l.contains(" for alice from")).findFirst().orElseThrow();
        out.append("  THE LINES (one of each source, about alice):\n");
        for (String[] l : new String[][] {{"web (JSON)", webLine}, {"database audit (key=value)", dbLine}, {"host (syslog)", hostLine}}) {
            out.append("    " + l[0] + "\n");
            wrap(l[1], 56).forEach(w -> out.append("      " + w + "\n"));
        }
        List<Event> three = List.of(Parsers.web(List.of(webLine)).events().getFirst(), Parsers.db(List.of(dbLine), true).events().getFirst(),
                Parsers.host(List.of(hostLine), 2026).events().getFirst());
        out.append("  THE EVENTS (the common shape, time in UTC):\n");
        String row = "    %-9s %-14s %-14s %s%n";
        out.append(String.format(Locale.ROOT, row, "field", "web", "database", "host"));
        List<Map.Entry<String, Function<Event, Object>>> fields = List.of(
                Map.entry("time", e -> clock(e.at())), Map.entry("source", Event::source), Map.entry("host", Event::host), Map.entry("user", Event::user),
                Map.entry("address", Event::ip), Map.entry("action", Event::action), Map.entry("outcome", Event::outcome), Map.entry("rows", Event::rows));
        for (var f : fields) {
            out.append(String.format(Locale.ROOT, row, f.getKey(), f.getValue().apply(three.get(0)), f.getValue().apply(three.get(1)), f.getValue().apply(three.get(2))));
        }
        out.append("  THE WHOLE DAY:\n");
        out.append(String.format(Locale.ROOT, "    %-10s %6s %8s %13s%n", "source", "lines", "events", "dead letters"));
        out.append(String.format(Locale.ROOT, "    %-10s %6d %8d %13d%n", "web", lines.web().size(), web.events().size(), web.deadLetters().size()));
        out.append(String.format(Locale.ROOT, "    %-10s %6d %8d %13d%n", "database", lines.db().size(), db.events().size(), db.deadLetters().size()));
        out.append(String.format(Locale.ROOT, "    %-10s %6d %8d %13d%n", "host", lines.host().size(), host.events().size(), host.deadLetters().size()));
        out.append("    a dead letter, kept and counted:\n      " + db.deadLetters().getFirst() + "\n");

        // TWO: one query on the common fields
        out.append("TWO. One query on the common fields.\n");
        long failedWebOnly = web.events().stream().filter(Event::failedLogin).count();
        out.append("  BEFORE NORMALIZING (a query on the web's own fields):\n");
        out.append("    type = login and ok = false\n");
        out.append("    failed logins found: " + failedWebOnly + " (the web only)\n");
        out.append("  AFTER NORMALIZING (one query on the common fields):\n");
        out.append("    action = login and outcome = failure\n");
        for (String source : List.of("web", "db", "host")) {
            out.append(String.format(Locale.ROOT, "    %-6s %d%n", source, events.stream().filter(Event::failedLogin).filter(e -> e.source().equals(source)).count()));
        }
        out.append("    failed logins found: " + events.stream().filter(Event::failedLogin).count() + " from "
                + events.stream().filter(Event::failedLogin).map(Event::source).distinct().count() + " sources\n");

        // THREE: the clock
        out.append("THREE. The clock: one offset decides an alert.\n");
        Parsed dbWrong = Parsers.db(lines.db(), false);
        Event bulk = db.events().stream().filter(e -> e.action().equals("bulk-read") && e.user().equals("alice")).findFirst().orElseThrow();
        Event bulkWrong = dbWrong.events().stream().filter(e -> e.action().equals("bulk-read") && e.user().equals("alice")).findFirst().orElseThrow();
        Event login = web.events().stream().filter(e -> e.user().equals("alice") && e.outcome().equals("success")).findFirst().orElseThrow();
        Duration fifteen = Duration.ofMinutes(15);
        String stamp = dbLine.substring("time=".length(), dbLine.indexOf(" db="));
        out.append("  THE DATABASE LINE (local time, with its offset):\n");
        out.append("    time=" + stamp + " ... rows=" + bulk.rows() + "\n");
        out.append("    the rule: a web login, then a bulk read within " + fifteen.toMinutes() + " min\n");
        out.append("    alice's web login: " + clock(login.at()) + " UTC\n");
        out.append("  HOW THE TIME IS MADE:\n");
        out.append("    offset honoured  " + stamp.substring(11, 19) + " - " + stamp.substring(21, 23) + ":" + stamp.substring(23, 25) + " = " + clock(bulk.at()) + " UTC\n");
        out.append("    offset ignored   " + stamp.substring(11, 19) + " read as UTC = " + clock(bulkWrong.at()) + " UTC\n");
        out.append("    the event moves by " + Duration.between(bulk.at(), bulkWrong.at()).abs().toMinutes() + " minutes\n");
        out.append("  THE RULE, BOTH WAYS:\n");
        out.append("    honoured: login " + clock(login.at()) + ", read " + clock(bulk.at()) + ", " + Duration.between(login.at(), bulk.at()).toMinutes() + " min apart: "
                + Correlator.loginThenBulkRead(events, fifteen).stream().filter(a -> a.user().equals("alice")).count() + " alert\n");
        out.append("    ignored:  login " + clock(login.at()) + ", read " + clock(bulkWrong.at()) + ", " + Duration.between(login.at(), bulkWrong.at()).toMinutes() + " min apart: "
                + Correlator.loginThenBulkRead(all(web, dbWrong, host), fifteen).size() + " alerts\n");
        Event report = db.events().stream().filter(e -> e.user().equals("svc-report")).findFirst().orElseThrow();
        out.append("    the nightly report job: " + report.rows() + " rows at " + clock(report.at()) + ", no web login\n");
        out.append("      before it: " + Correlator.loginThenBulkRead(events, fifteen).stream().filter(a -> a.user().equals("svc-report")).count() + " alerts\n");

        // FOUR: correlating across sources
        out.append("FOUR. Correlating across sources.\n");
        Duration ten = Duration.ofMinutes(10);
        List<Event> attempt = events.stream().filter(e -> e.user().equals("alice") && e.ip().equals(ATTACKER)).toList();
        out.append("  THE EVENTS FOR alice FROM " + ATTACKER + ":\n");
        for (String source : List.of("web", "host")) {
            List<Event> failed = attempt.stream().filter(e -> e.source().equals(source) && e.failedLogin()).toList();
            out.append(String.format(Locale.ROOT, "    %-5s", source) + failed.size() + " failed logins, " + clock(failed.getFirst().at()) + " to " + clock(failed.getLast().at()) + "\n");
        }
        Event success = attempt.stream().filter(e -> e.outcome().equals("success")).findFirst().orElseThrow();
        out.append(String.format(Locale.ROOT, "    %-5s", success.source()) + "a successful login at " + clock(success.at()) + "\n");
        out.append("  HOW THE RULE COUNTS (10 failures within 10 min, then a success):\n");
        Instant from = success.at().minus(ten);
        out.append("    the window: " + clock(from) + " to " + clock(success.at()) + "\n");
        for (String source : List.of("web", "host")) {
            long n = attempt.stream().filter(e -> e.source().equals(source) && e.failedLogin() && !e.at().isBefore(from)).count();
            boolean ok = attempt.stream().anyMatch(e -> e.source().equals(source) && e.outcome().equals("success"));
            out.append(String.format(Locale.ROOT, "    %-12s", source + " alone:") + n + " failures" + (ok ? "" : ", no success") + ", under 10: no alert\n");
        }
        List<Alert> joined = Correlator.bruteForce(events, 10, ten);
        long both = attempt.stream().filter(e -> e.failedLogin() && !e.at().isBefore(from)).count();
        out.append(String.format(Locale.ROOT, "    %-12s", "joined:") + both + " failures, then the success: alert\n");
        out.append("  THE RESULT:\n");
        out.append("    one source at a time: " + Correlator.perSource(events, 10, ten).size() + " alerts\n");
        out.append("    the joined events:    " + joined.size() + " alert, " + joined.getFirst().user() + ", " + joined.getFirst().events() + " events behind it\n");
        out.append("    everyone else (up to 3 mistyped passwords): "
                + Correlator.bruteForce(events.stream().filter(e -> !e.user().equals("alice")).toList(), 10, ten).size() + " alerts\n");

        // FIVE: from alerts to cases
        out.append("FIVE. From alerts to cases.\n");
        List<Alert> alerts = new ArrayList<>(joined);
        alerts.addAll(Correlator.loginThenBulkRead(events, fifteen).stream().filter(a -> !a.user().equals("svc-report")).toList());
        out.append("  THE ALERTS:\n");
        alerts.forEach(a -> out.append(String.format(Locale.ROOT, "    %-36s %-6s %2d events%n", a.rule(), a.user(), a.events())));
        Map<String, List<Alert>> cases = Correlator.cases(alerts);
        out.append("  THE CASES (alerts about one user are one case):\n");
        cases.forEach((user, list) -> out.append("    " + user + ": " + list.size() + " alerts, " + list.stream().mapToInt(Alert::events).sum() + " events, one story\n"));
        out.append("    alerts " + alerts.size() + ", cases " + cases.size() + "\n");
        return out.toString();
    }
}
