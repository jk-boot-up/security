package com.jk.explore.security.siembasics;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the lines of each source into events. A line that does not fit goes to the dead letters instead of being dropped without a trace. */
public final class Parsers {

    /** What a parser produced. */
    public record Parsed(List<Event> events, List<String> deadLetters) {
    }

    private static final Pattern WEB = Pattern.compile("\\{\"ts\":\"([^\"]+)\",\"type\":\"(\\w+)\",\"user\":\"([^\"]+)\",\"ip\":\"([^\"]+)\",\"ok\":(true|false)\\}");
    private static final Pattern DB = Pattern.compile("time=(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d [+-]\\d{4}) db=\\w+ actor=(\\S+) client=(\\S+) stmt=(\\w+) rows=(\\d+) result=(\\w+)");
    private static final Pattern HOST = Pattern.compile("(\\w{3} +\\d+ \\d\\d:\\d\\d:\\d\\d) (\\S+) sshd\\[\\d+\\]: (Accepted|Failed) password for (\\S+) from (\\S+) port \\d+ ssh2");
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx");
    private static final DateTimeFormatter SYSLOG = DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss", Locale.ENGLISH);

    private Parsers() {
    }

    public static Parsed web(List<String> lines) {
        List<Event> events = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        for (String l : lines) {
            Matcher m = WEB.matcher(l);
            if (!m.matches()) {
                dead.add(l);
                continue;
            }
            events.add(new Event(Instant.parse(m.group(1)), "web", "web-01", m.group(3), m.group(4), m.group(2), m.group(5).equals("true") ? "success" : "failure", 0));
        }
        return new Parsed(events, dead);
    }

    /** The database writes local time with its offset; {@code honourOffset} false reads the clock as if it were UTC, which is the mistake. */
    public static Parsed db(List<String> lines, boolean honourOffset) {
        List<Event> events = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        for (String l : lines) {
            Matcher m = DB.matcher(l);
            if (!m.matches()) {
                dead.add(l);
                continue;
            }
            Instant at = honourOffset ? OffsetDateTime.parse(m.group(1), DB_TIME).toInstant() : LocalDateTime.parse(m.group(1).substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC);
            events.add(new Event(at, "db", "db-01", m.group(2), m.group(3), m.group(4).equals("SELECT") && Long.parseLong(m.group(5)) > 1000 ? "bulk-read" : "query", m.group(6).equals("ok") ? "success" : "failure", Long.parseLong(m.group(5))));
        }
        return new Parsed(events, dead);
    }

    /** Syslog has no year and no zone: the year comes from the collector and the zone from the host's documented setting (UTC here). */
    public static Parsed host(List<String> lines, int year) {
        List<Event> events = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        for (String l : lines) {
            Matcher m = HOST.matcher(l);
            if (!m.matches()) {
                dead.add(l);
                continue;
            }
            Instant at = LocalDateTime.parse(year + " " + m.group(1).replaceAll(" +", " "), SYSLOG).toInstant(ZoneOffset.UTC);
            events.add(new Event(at, "host", m.group(2), m.group(4), m.group(5), "login", m.group(3).equals("Accepted") ? "success" : "failure", 0));
        }
        return new Parsed(events, dead);
    }
}
