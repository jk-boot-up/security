package com.jk.explore.security.securitylogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
public class SecurityLoggingDemo {

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

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
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

    private static void act1WhatToLog(StringBuilder out) throws IOException {
        line(out, "ONE. What a security log must answer.");
        List<String> old = oldLog();
        line(out, "  the bank's old logger wrote " + old.size() + " lines this morning, for example:");
        line(out, "    " + old.get(1));
        line(out, "    " + old.get(5));
        line(out, "  the question: who moved 500 from ANA-1 to MAL-9, and from where?");
        line(out, "  old lines that mention ANA-1: " + LogQuery.plainLinesMentioning(old, "ANA-1") + "  <- THE LOG CANNOT ANSWER");
        List<String> log = securityLog();
        line(out, "  the security log records who, what, when, where from, the outcome and a request id for each of " + log.size() + " events");
        Map<String, String> transfer = LogQuery.where(log, "target", "ANA-1 -> MAL-9").getFirst();
        line(out, "  the transfer: by " + transfer.get("actor") + ", from " + transfer.get("source") + ", at " + transfer.get("time")
                + ", " + transfer.get("outcome") + ", request " + transfer.get("correlation"));
        List<Map<String, String>> sameAddress = LogQuery.where(log, "source", transfer.get("source"));
        long failed = sameAddress.stream().filter(f -> f.get("type").equals("login") && f.get("outcome").equals("failure")).count();
        long succeeded = sameAddress.stream().filter(f -> f.get("type").equals("login") && f.get("outcome").equals("success")).count();
        line(out, "  the same address before it: " + failed + " failed logins as ana, then " + succeeded
                + " success  <- SOMEONE WITH ANA'S PASSWORD, NOT ANA");
        line(out, "  event types in the log: " + log.stream().map(l -> Json.parse(l).get("type")).distinct().toList());
    }

    private static void act2WhatNeverToLog(StringBuilder out) throws IOException {
        line(out, "TWO. What never to log.");
        List<String> old = oldLog();
        line(out, "  the old logger's card payment line holds the full card number: " + old.getLast().contains(BankDay.CARD)
                + "  <- A BREACH OF THE LOG IS A BREACH OF THE CARDS");
        List<String> log = securityLog();
        Map<String, String> failedLogin = Json.parse(log.get(1));
        Map<String, String> session = Json.parse(log.get(3));
        Map<String, String> transfer = Json.parse(log.get(5));
        Map<String, String> payment = Json.parse(log.get(7));
        line(out, "  the password tried in a failed login: " + failedLogin.get("detail.password"));
        line(out, "  the session id of the new login: " + session.get("detail.session"));
        line(out, "  the transfer's Authorization header: " + transfer.get("detail.authorization"));
        line(out, "  the card payment: card " + payment.get("detail.card") + ", and a card number typed into the note: \""
                + payment.get("detail.note") + "\"");
        long found = BankDay.SECRETS.stream().filter(secret -> log.stream().anyMatch(l -> l.contains(secret))).count();
        line(out, "  secrets from the morning's events found anywhere in the security log: " + found + " of " + BankDay.SECRETS.size());
        line(out, "  secrets are removed by field name, and card numbers and tokens also by their shape, so free text is covered too");
    }

    private static void act3Injection(StringBuilder out) throws IOException {
        line(out, "THREE. Text that tries to write the log.");
        String typed = "mallory\n2026-10-18T09:09:00Z INFO login ok user=admin";
        line(out, "  a username typed into the login form: \"" + typed.replace("\n", "\\n") + "\"");
        SimClock clock = new SimClock(Instant.parse("2026-10-18T09:08:00Z"));
        PlainLogger plain = new PlainLogger(clock);
        plain.error("login failed user=" + typed);
        List<String> plainLines = plain.lines();
        line(out, "  the old logger's file now has " + plainLines.size() + " lines; the second reads: " + plainLines.get(1)
                + "  <- A FORGED LINE");
        MemorySink sink = new MemorySink();
        SecurityLog log = new SecurityLog(sink);
        log.record(failedLogin(clock, typed, "req-0009"));
        Map<String, String> read = Json.parse(sink.lines().getFirst());
        line(out, "  the security log wrote " + sink.lines().size() + " line; read back, the actor is exactly what was typed: "
                + read.get("actor").equals(typed) + ", and the outcome is " + read.get("outcome"));
        String quoted = "mallory\",\"outcome\":\"success";
        log.record(failedLogin(clock, quoted, "req-0010"));
        Map<String, String> second = Json.parse(sink.lines().getLast());
        line(out, "  a username carrying a quote and a fake field: the outcome still reads " + second.get("outcome")
                + ", and the actor is the text as typed: " + second.get("actor").equals(quoted));
        line(out, "  structured output escapes text instead of trusting it, and keeps it whole, so investigators see exactly what was sent");
    }

    private static SecurityEvent failedLogin(SimClock clock, String user, String request) {
        return new SecurityEvent(clock.now(), EventType.LOGIN, user, "login", user, "192.0.2.44", Outcome.FAILURE, request,
                Map.of("reason", "unknown user"));
    }

    private static void act4TamperEvidence(StringBuilder out, Path dir) throws IOException {
        line(out, "FOUR. A log that shows it was changed.");
        FileSink file = new FileSink(dir.resolve("security.log"));
        SecurityLog log = new SecurityLog(file);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        List<String> lines = file.lines();
        line(out, "  each line carries the SHA-256 of the line before it; the morning's " + lines.size() + " lines: "
                + describe(ChainCheck.verify(lines, SecurityLog.GENESIS)));
        List<String> deleted = new ArrayList<>(lines);
        deleted.remove(5);
        line(out, "  line 6, the transfer, deleted: " + describe(ChainCheck.verify(deleted, SecurityLog.GENESIS)));
        List<String> edited = new ArrayList<>(lines);
        edited.set(5, lines.get(5).replace("\"detail.amount\":\"500\"", "\"detail.amount\":\"50\""));
        line(out, "  line 6's amount edited from 500 to 50: " + describe(ChainCheck.verify(edited, SecurityLog.GENESIS)));
        List<String> lastEdited = new ArrayList<>(lines);
        lastEdited.set(7, lines.get(7).replace("\"detail.amount\":\"80\"", "\"detail.amount\":\"8\""));
        ChainCheck.Result tail = ChainCheck.verify(lastEdited, SecurityLog.GENESIS);
        line(out, "  the last line edited: " + describe(tail) + "  <- NOTHING FOLLOWS THE LAST LINE TO GIVE IT AWAY");
        line(out, "  compared with the latest hash, published somewhere else as each line is written: "
                + (tail.head().equals(log.head()) ? "matches" : "does not match, so the change is found"));
        line(out, "  the file is written by appending only, with permissions " + file.permissions());
        line(out, "  an attacker with root can still rewrite the whole file and its chain, which is why the log must also leave the host");
    }

    private static String describe(ChainCheck.Result result) {
        return result.intact() ? "intact" : "broken at line " + result.brokenAt();
    }

    private static void act5TheBill(StringBuilder out) throws IOException {
        line(out, "FIVE. The bill.");
        MemorySink local = new MemorySink();
        MemorySink shipped = new MemorySink();
        SecurityLog log = new SecurityLog(local, shipped);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        local.clear();
        line(out, "  every line is also shipped to a second store as it is written; the attacker deletes the local log: local "
                + local.lines().size() + " lines, shipped " + shipped.lines().size() + " lines, "
                + describe(ChainCheck.verify(shipped.lines(), SecurityLog.GENESIS)));
        MemorySink year = new MemorySink();
        SecurityLog daily = new SecurityLog(year);
        SimClock clock = new SimClock(Instant.parse("2026-01-01T12:00:00Z"));
        for (int day = 1; day <= 40; day++) {
            daily.record(new SecurityEvent(clock.now(), EventType.LOGIN, "customer-" + day, "login", "customer-" + day, "203.0.113.7",
                    Outcome.SUCCESS, "req-d%02d".formatted(day), Map.of()));
            clock.advance(Duration.ofDays(1));
        }
        Retention.Pruned pruned = Retention.prune(year.lines(), clock.now(), Duration.ofDays(30));
        line(out, "  names and addresses make the log personal data, so it is kept for a fixed time: keeping 30 days, on day 41 "
                + pruned.removed() + " old lines go and " + pruned.kept().size() + " stay");
        line(out, "  the kept lines check from the anchor written down when the old ones went: "
                + describe(ChainCheck.verify(pruned.kept(), pruned.anchor())));
        List<String> morning = securityLog();
        long average = Math.round(morning.stream().mapToInt(l -> l.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1)
                .average().orElse(0));
        line(out, "  the morning's lines average " + average + " bytes, so 2000000 logins a day write about "
                + Math.round(average * 2_000_000 / 1_000_000.0) + " megabytes a day for logins alone: volume needs a budget");
        line(out, "  and a log that nobody reads is evidence only after the fact: the -with-loki-and-grafana version ships, queries and alerts");
    }
}
