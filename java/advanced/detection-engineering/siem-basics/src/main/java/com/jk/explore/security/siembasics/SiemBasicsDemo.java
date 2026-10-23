package com.jk.explore.security.siembasics;

import com.jk.explore.security.siembasics.Correlator.Alert;
import com.jk.explore.security.siembasics.Parsers.Parsed;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SiemBasicsDemo {

    static final long SEED = 20261023L;

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

    public static String run() {
        StringBuilder out = new StringBuilder();
        Sources.Lines lines = Sources.day(SEED);
        Parsed web = Parsers.web(lines.web());
        Parsed db = Parsers.db(lines.db(), true);
        Parsed host = Parsers.host(lines.host(), 2026);

        out.append("--- collecting three sources\n");
        out.append("lines: web (JSON) " + lines.web().size() + ", database audit (key=value) " + lines.db().size() + ", host (syslog) " + lines.host().size() + "\n");
        out.append("events parsed: web " + web.events().size() + ", database " + db.events().size() + ", host " + host.events().size() + "\n");
        out.append("lines that did not fit and were kept as dead letters: web " + web.deadLetters().size() + ", database " + db.deadLetters().size() + ", host " + host.deadLetters().size() + "\n");

        out.append("--- normalizing into one shape\n");
        List<Event> events = all(web, db, host);
        long failedWebOnly = web.events().stream().filter(Event::failedLogin).count();
        long failedAll = events.stream().filter(Event::failedLogin).count();
        out.append("failed logins found by a query written for the web application's fields: " + failedWebOnly + "\n");
        out.append("failed logins found by one query on the common fields (action login, outcome failure): " + failedAll + " from " + events.stream().filter(Event::failedLogin).map(Event::source).distinct().count() + " sources\n");
        out.append("the common shape: time in UTC, source, host, user, address, action, outcome\n");

        out.append("--- the clock\n");
        Parsed dbWrong = Parsers.db(lines.db(), false);
        Event bulk = db.events().stream().filter(e -> e.action().equals("bulk-read") && e.user().equals("alice")).findFirst().orElseThrow();
        Event bulkWrong = dbWrong.events().stream().filter(e -> e.action().equals("bulk-read") && e.user().equals("alice")).findFirst().orElseThrow();
        out.append("the database writes local time with an offset of +0530; read as if it were UTC the event moves by " + Duration.between(bulk.at(), bulkWrong.at()).abs().toMinutes() + " minutes\n");
        Duration fifteen = Duration.ofMinutes(15);
        out.append("rule \"login, then a bulk read within 15 minutes\" with the offset honoured: " + Correlator.loginThenBulkRead(events, fifteen).size() + " alert, with the offset ignored: " + Correlator.loginThenBulkRead(all(web, dbWrong, host), fifteen).size() + " alerts\n");
        out.append("the nightly report job reads 600000 rows at 02:00 and has no web login before it: alerts for it " + Correlator.loginThenBulkRead(events, fifteen).stream().filter(a -> a.user().equals("svc-report")).count() + "\n");

        out.append("--- correlating across sources\n");
        out.append("a rule of 10 failed logins within 10 minutes, one source at a time: " + Correlator.perSource(events, 10, Duration.ofMinutes(10)).size() + " alerts\n");
        List<Alert> joined = Correlator.bruteForce(events, 10, Duration.ofMinutes(10));
        out.append("the same rule on the joined events: " + joined.size() + " alert, " + joined.get(0).user() + ", " + joined.get(0).events() + " events behind it\n");
        out.append("ordinary mistyped passwords (up to 3 failures before a success) raise: " + Correlator.bruteForce(events.stream().filter(e -> !e.user().equals("alice")).toList(), 10, Duration.ofMinutes(10)).size() + " alerts\n");

        out.append("--- from alerts to cases\n");
        List<Alert> alerts = new ArrayList<>(joined);
        alerts.addAll(Correlator.loginThenBulkRead(events, fifteen).stream().filter(a -> !a.user().equals("svc-report")).toList());
        Map<String, List<Alert>> cases = Correlator.cases(alerts);
        out.append("alerts: " + alerts.size() + ", cases (one for each user): " + cases.size() + ", the case: " + cases.keySet() + " with " + cases.values().iterator().next().size() + " alerts\n");

        out.append("--- the bill\n");
        out.append("a source nobody parses is a source nobody searches: keep the lines that do not fit, and count them\n");
        out.append("time is the first thing that breaks a correlation: normalize it to UTC and record where each clock came from\n");
        out.append("a rule that looks at one source at a time sees only part of an attack\n");
        return out.toString();
    }
}
