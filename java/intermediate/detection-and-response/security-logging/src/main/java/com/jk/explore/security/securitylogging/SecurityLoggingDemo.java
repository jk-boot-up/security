package com.jk.explore.security.securitylogging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Each act prints labelled sections: the
 * input first, then the result, and any derived value worked out step by step.
 */
public class SecurityLoggingDemo {

    /** How many hex digits of a SHA-256 the demo prints: enough to tell the hashes apart. */
    static final int SHORT_HASH = 12;

    /** The fields of the transfer the first act shows, in the order a security event holds them. */
    private static final List<String> EVENT_FIELDS = List.of("time", "type", "actor", "action", "target", "source", "outcome",
            "correlation");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("security-logging");
        try {
            act1WhatToLog(out);
            act2WhatNeverToLog(out);
            act3Injection(out);
            act4TamperEvidence(out, dir);
            act5TheBill(out);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
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

    static String shortHash(String hash) {
        return hash.substring(0, SHORT_HASH);
    }

    /** The morning's events written to a fresh security log in memory. */
    static List<String> securityLog() throws IOException {
        MemorySink sink = new MemorySink();
        SecurityLog log = new SecurityLog(sink);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        return sink.lines();
    }

    static List<String> oldLog() {
        SimClock clock = new SimClock();
        PlainLogger plain = new PlainLogger(clock);
        BankDay.oldLogger(plain, clock);
        return plain.lines();
    }

    private static String clockTime(String isoTime) {
        return isoTime.substring(11, 16);
    }

    private static void act1WhatToLog(StringBuilder out) throws IOException {
        act(out, "ONE. What a security log must answer.");
        List<String> old = oldLog();
        section(out, "THE OLD LOG (" + old.size() + " lines this morning, lines 2 and 6)");
        row(out, old.get(1));
        row(out, old.get(5));
        section(out, "THE QUESTION");
        row(out, "who moved 500 from ANA-1 to MAL-9, and from where?");
        section(out, "THE OLD LOG'S ANSWER");
        row(out, "lines that mention ANA-1: " + LogQuery.plainLinesMentioning(old, "ANA-1") + "  <- IT CANNOT ANSWER");
        List<String> log = securityLog();
        Map<String, String> transfer = LogQuery.where(log, "target", "ANA-1 -> MAL-9").getFirst();
        section(out, "THE SAME TRANSFER IN THE SECURITY LOG (line 6, its fields)");
        for (String field : EVENT_FIELDS) {
            fact(out, 13, field, transfer.get(field));
        }
        section(out, "THE SECURITY LOG'S ANSWER (every event as " + transfer.get("actor") + ")");
        for (Map<String, String> f : LogQuery.where(log, "actor", transfer.get("actor"))) {
            row(out, clockTime(f.get("time")) + "  " + pad(f.get("source"), 15) + pad(f.get("type"), 16) + f.get("outcome"));
        }
        List<Map<String, String>> sameAddress = LogQuery.where(log, "source", transfer.get("source"));
        long failed = sameAddress.stream().filter(f -> f.get("type").equals("login") && f.get("outcome").equals("failure")).count();
        long succeeded = sameAddress.stream().filter(f -> f.get("type").equals("login") && f.get("outcome").equals("success")).count();
        String usual = Json.parse(log.getFirst()).get("source");
        row(out, "from " + transfer.get("source") + ": " + failed + " failed logins, " + succeeded + " success, the transfer");
        row(out, "ana's usual address is " + usual + "  <- ANA'S PASSWORD, NOT ANA");
        section(out, "THE EVENT TYPES (" + log.size() + " events)");
        Map<String, Long> types = log.stream().map(l -> Json.parse(l).get("type"))
                .collect(Collectors.groupingBy(t -> t, LinkedHashMap::new, Collectors.counting()));
        row(out, types.entrySet().stream().map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", ")));
    }

    private static void act2WhatNeverToLog(StringBuilder out) throws IOException {
        act(out, "TWO. What never to log.");
        List<String> old = oldLog();
        section(out, "THE OLD LOG'S CARD PAYMENT (line 8)");
        row(out, old.getLast());
        row(out, "the full card number: " + old.getLast().contains(BankDay.CARD) + "  <- A LEAKED LOG LEAKS THE CARD");
        List<SecurityEvent> events = BankDay.events(new SimClock());
        List<String> log = securityLog();
        String[][] secretFields = {{"2", "password"}, {"4", "session"}, {"6", "authorization"}, {"8", "card"}, {"8", "note"}};
        section(out, "BEFORE THE REDACTOR (the events' details, fake values)");
        for (String[] at : secretFields) {
            String value = events.get(Integer.parseInt(at[0]) - 1).details().get(at[1]);
            if (value.startsWith("Bearer ")) {
                row(out, "line " + at[0] + "  " + pad(at[1], 15) + "Bearer, then the token:");
                row(out, "  " + value.substring("Bearer ".length()));
            } else {
                row(out, "line " + at[0] + "  " + pad(at[1], 15) + value);
            }
        }
        section(out, "AFTER THE REDACTOR (the same fields in the security log)");
        for (String[] at : secretFields) {
            row(out, "line " + at[0] + "  " + pad(at[1], 15) + Json.parse(log.get(Integer.parseInt(at[0]) - 1)).get("detail." + at[1]));
        }
        section(out, "HOW EACH ONE WAS CAUGHT");
        String card = BankDay.CARD;
        row(out, "by field name  password, session, authorization");
        row(out, "by its shape   " + card + ": " + card.length() + " digits, keep the last 4");
        row(out, "               " + (card.length() - 4) + " x * + " + card.substring(card.length() - 4) + " = "
                + Redactor.text(card));
        row(out, "by its shape   the note's 4111-1111-1111-1111: hyphens ignored");
        section(out, "THE SEARCH (every fake secret, in the security log)");
        long found = BankDay.SECRETS.stream().filter(secret -> log.stream().anyMatch(l -> l.contains(secret))).count();
        row(out, "found " + found + " of " + BankDay.SECRETS.size());
    }

    private static void act3Injection(StringBuilder out) throws IOException {
        act(out, "THREE. Text that tries to write the log.");
        String typed = "mallory\n2026-10-18T09:09:00Z INFO login ok user=admin";
        section(out, "THE INPUT (a username typed into the login form, \\n shown)");
        row(out, typed.replace("\n", "\\n"));
        SimClock clock = new SimClock(Instant.parse("2026-10-18T09:08:00Z"));
        PlainLogger plain = new PlainLogger(clock);
        plain.error("login failed user=" + typed);
        List<String> plainLines = plain.lines();
        section(out, "THE OLD LOG (1 failed login, " + plainLines.size() + " lines in the file)");
        row(out, "1 " + plainLines.get(0));
        row(out, "2 " + plainLines.get(1) + "  <- FORGED");
        MemorySink sink = new MemorySink();
        SecurityLog log = new SecurityLog(sink);
        String first = log.record(failedLogin(clock, typed, "req-0009"));
        section(out, "THE SECURITY LOG (" + sink.lines().size() + " line; its actor field as written)");
        row(out, field(first, "actor"));
        Map<String, String> read = Json.parse(first);
        section(out, "READ BACK");
        fact(out, 9, "actor", "exactly what was typed: " + read.get("actor").equals(typed));
        fact(out, 9, "outcome", read.get("outcome"));
        String quoted = "mallory\",\"outcome\":\"success";
        section(out, "THE INPUT (a username with quotes and a fake field)");
        row(out, quoted);
        String second = log.record(failedLogin(clock, quoted, "req-0010"));
        section(out, "THE SECURITY LOG (its actor and outcome fields as written)");
        row(out, field(second, "actor"));
        row(out, field(second, "outcome"));
        Map<String, String> back = Json.parse(second);
        section(out, "READ BACK");
        fact(out, 9, "actor", "exactly what was typed: " + back.get("actor").equals(quoted));
        fact(out, 9, "outcome", back.get("outcome"));
        section(out, "HOW THE TEXT IS ESCAPED");
        row(out, "line break  ->  \\n      quote  ->  \\\"      backslash  ->  \\\\");
    }

    /** One field of a written line, exactly as it appears in the line: "name":"escaped value". */
    static String field(String line, String name) {
        String start = "\"" + name + "\":\"";
        int from = line.indexOf(start);
        int end = from + start.length();
        while (line.charAt(end) != '"' || line.charAt(end - 1) == '\\') {
            end++;
        }
        return line.substring(from, end + 1);
    }

    private static SecurityEvent failedLogin(SimClock clock, String user, String request) {
        return new SecurityEvent(clock.now(), EventType.LOGIN, user, "login", user, "192.0.2.44", Outcome.FAILURE, request,
                Map.of("reason", "unknown user"));
    }

    private static void act4TamperEvidence(StringBuilder out, Path dir) throws IOException {
        act(out, "FOUR. A log that shows it was changed.");
        FileSink file = new FileSink(dir.resolve("security.log"));
        SecurityLog log = new SecurityLog(file);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        List<String> lines = file.lines();
        section(out, "THE CHAIN (each line's prev, the first " + SHORT_HASH + " hex digits)");
        for (int i = 0; i < lines.size(); i++) {
            String prev = Json.parse(lines.get(i)).get("prev");
            row(out, "line " + (i + 1) + "  prev " + shortHash(prev)
                    + (i == 0 ? "  (the start: all zeros)" : "  = SHA-256 of line " + i));
        }
        row(out, pad("head", 13) + shortHash(log.head()) + "  = SHA-256 of line " + lines.size() + ", kept elsewhere");
        List<String> edited = new ArrayList<>(lines);
        edited.set(5, lines.get(5).replace("\"detail.amount\":\"500\"", "\"detail.amount\":\"50\""));
        section(out, "HOW AN EDIT IS FOUND (line 6's amount, 500 -> 50)");
        fact(out, 26, "SHA-256 of line 6 before", shortHash(Hashes.sha256(lines.get(5))));
        fact(out, 26, "SHA-256 of line 6 after", shortHash(Hashes.sha256(edited.get(5))));
        fact(out, 26, "line 7's prev", shortHash(Json.parse(lines.get(6)).get("prev")) + "  <- NO LONGER EQUAL");
        List<String> deleted = new ArrayList<>(lines);
        deleted.remove(5);
        List<String> lastEdited = new ArrayList<>(lines);
        lastEdited.set(7, lines.get(7).replace("\"detail.amount\":\"80\"", "\"detail.amount\":\"8\""));
        ChainCheck.Result tail = ChainCheck.verify(lastEdited, SecurityLog.GENESIS);
        section(out, "THE CHECKS (the log as found, and the result)");
        fact(out, 26, "the morning's " + lines.size() + " lines", describe(ChainCheck.verify(lines, SecurityLog.GENESIS)));
        fact(out, 26, "line 6 deleted", describe(ChainCheck.verify(deleted, SecurityLog.GENESIS)));
        fact(out, 26, "line 6 amount 500 -> 50", describe(ChainCheck.verify(edited, SecurityLog.GENESIS)));
        fact(out, 26, "line 8 amount 80 -> 8", describe(tail) + "  <- NOTHING FOLLOWS IT");
        fact(out, 26, "line 8, against the head", tail.head().equals(log.head()) ? "matches" : "does not match: found");
        section(out, "THE FILE");
        fact(out, 13, "written by", "appending only");
        fact(out, 13, "permissions", file.permissions() + " (owner only)");
        fact(out, 13, "but root", "can rewrite the file and its chain");
    }

    private static String describe(ChainCheck.Result result) {
        return result.intact() ? "intact" : "broken at line " + result.brokenAt();
    }

    private static void act5TheBill(StringBuilder out) throws IOException {
        act(out, "FIVE. The bill.");
        MemorySink local = new MemorySink();
        MemorySink shipped = new MemorySink();
        SecurityLog log = new SecurityLog(local, shipped);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        section(out, "BEFORE THE ATTACKER (each line goes to both stores)");
        fact(out, 9, "local", local.lines().size() + " lines");
        fact(out, 9, "shipped", shipped.lines().size() + " lines");
        local.clear();
        section(out, "AFTER THE ATTACKER DELETES THE LOCAL LOG");
        fact(out, 9, "local", local.lines().size() + " lines");
        fact(out, 9, "shipped", shipped.lines().size() + " lines, chain "
                + describe(ChainCheck.verify(shipped.lines(), SecurityLog.GENESIS)));

        MemorySink year = new MemorySink();
        SecurityLog daily = new SecurityLog(year);
        Instant first = Instant.parse("2026-01-01T12:00:00Z");
        SimClock clock = new SimClock(first);
        int days = 40;
        for (int day = 1; day <= days; day++) {
            daily.record(new SecurityEvent(clock.now(), EventType.LOGIN, "customer-" + day, "login", "customer-" + day, "203.0.113.7",
                    Outcome.SUCCESS, "req-d%02d".formatted(day), Map.of()));
            clock.advance(Duration.ofDays(1));
        }
        Duration keep = Duration.ofDays(30);
        Retention.Pruned pruned = Retention.prune(year.lines(), clock.now(), keep);
        section(out, "RETENTION (one login a day for " + days + " days, keep " + keep.toDays() + " days)");
        fact(out, 10, "first", first.toString().substring(0, 10) + " (day 1)");
        fact(out, 10, "today", clock.now().toString().substring(0, 10) + " (day " + (days + 1) + ")");
        fact(out, 10, "cutoff", "today - " + keep.toDays() + " days = " + clock.now().minus(keep).toString().substring(0, 10));
        fact(out, 10, "removed", pruned.removed() + " lines, day 1 to day " + pruned.removed());
        fact(out, 10, "kept", pruned.kept().size() + " lines, day " + (pruned.removed() + 1) + " to day " + days);
        fact(out, 10, "anchor", shortHash(pruned.anchor()) + " = the first kept line's prev");
        fact(out, 10, "check", "from the anchor: " + describe(ChainCheck.verify(pruned.kept(), pruned.anchor())));

        List<String> morning = securityLog();
        int bytes = morning.stream().mapToInt(l -> l.getBytes(StandardCharsets.UTF_8).length + 1).sum();
        long average = Math.round(bytes / (double) morning.size());
        long logins = 2_000_000;
        section(out, "THE VOLUME (worked out from the morning's lines)");
        fact(out, 16, "the morning", morning.size() + " lines, " + bytes + " bytes");
        fact(out, 16, "one line", bytes + " / " + morning.size() + " = " + average + " bytes (rounded)");
        fact(out, 16, "logins a day", logins);
        fact(out, 16, "bytes a day", logins + " x " + average + " = " + logins * average);
        fact(out, 16, "that is about", Math.round(logins * average / 1_000_000.0) + " megabytes a day, logins only");
    }
}
