package com.jk.explore.security.siembasics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SiemTest {

    @Test
    void aWebLineBecomesAnEventInUtc() {
        Parsers.Parsed p = Parsers.web(List.of("{\"ts\":\"2026-10-23T09:12:03Z\",\"type\":\"login\",\"user\":\"alice\",\"ip\":\"203.0.113.5\",\"ok\":false}"));
        Event e = p.events().get(0);
        assertEquals(Instant.parse("2026-10-23T09:12:03Z"), e.at());
        assertTrue(e.failedLogin());
        assertEquals("203.0.113.5", e.ip());
    }

    @Test
    void aDatabaseLineWithAnOffsetIsMovedToUtcOrMisreadOnPurpose() {
        String line = "time=2026-10-23 14:42:11 +0530 db=ledger actor=alice client=10.0.0.8 stmt=SELECT rows=500000 result=ok";
        assertEquals(Instant.parse("2026-10-23T09:12:11Z"), Parsers.db(List.of(line), true).events().get(0).at());
        assertEquals(Instant.parse("2026-10-23T14:42:11Z"), Parsers.db(List.of(line), false).events().get(0).at());
        assertEquals("bulk-read", Parsers.db(List.of(line), true).events().get(0).action());
    }

    @Test
    void aSyslogLineGetsTheYearFromTheCollector() {
        Parsers.Parsed p = Parsers.host(List.of("Oct  3 09:12:08 bastion-01 sshd[1234]: Failed password for alice from 203.0.113.5 port 55123 ssh2"), 2026);
        assertEquals(Instant.parse("2026-10-03T09:12:08Z"), p.events().get(0).at());
        assertEquals("failure", p.events().get(0).outcome());
    }

    @Test
    void aLineThatDoesNotFitIsKeptAsADeadLetter() {
        Parsers.Parsed p = Parsers.db(List.of("time=garbled db=ledger actor=? stmt=1"), true);
        assertEquals(0, p.events().size());
        assertEquals(1, p.deadLetters().size());
    }

    private static Event login(String source, long second, String user, String ip, boolean ok) {
        return new Event(Instant.parse("2026-10-23T09:00:00Z").plusSeconds(second), source, "h", user, ip, "login", ok ? "success" : "failure", 0);
    }

    @Test
    void failuresFromTwoSourcesAddUpOnlyWhenTheEventsAreJoined() {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            events.add(login("web", i * 10, "alice", "1.1.1.1", false));
            events.add(login("host", i * 10 + 5, "alice", "1.1.1.1", false));
        }
        events.add(login("web", 200, "alice", "1.1.1.1", true));
        assertEquals(0, Correlator.perSource(events, 10, Duration.ofMinutes(10)).size());
        assertEquals(1, Correlator.bruteForce(events, 10, Duration.ofMinutes(10)).size());
    }

    @Test
    void failuresFromAnotherAddressDoNotCount() {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            events.add(login("web", i, "alice", "2.2.2.2", false));
        }
        events.add(login("web", 100, "alice", "1.1.1.1", true));
        assertEquals(0, Correlator.bruteForce(events, 10, Duration.ofMinutes(10)).size());
    }

    @Test
    void aBulkReadCountsOnlyAfterALoginByTheSameUserWithinTheWindow() {
        Event read = new Event(Instant.parse("2026-10-23T09:20:00Z"), "db", "db", "alice", "10.0.0.8", "bulk-read", "success", 500_000);
        List<Event> events = List.of(login("web", 0, "alice", "1.1.1.1", true), read);
        assertEquals(1, Correlator.loginThenBulkRead(events, Duration.ofMinutes(30)).size());
        assertEquals(0, Correlator.loginThenBulkRead(events, Duration.ofMinutes(5)).size());
        assertEquals(0, Correlator.loginThenBulkRead(List.of(read), Duration.ofMinutes(30)).size());
    }

    @Test
    void alertsAboutOneUserAreOneCase() {
        List<Correlator.Alert> alerts = List.of(new Correlator.Alert("a", "alice", 3), new Correlator.Alert("b", "alice", 2), new Correlator.Alert("a", "bob", 1));
        assertEquals(2, Correlator.cases(alerts).size());
        assertEquals(2, Correlator.cases(alerts).get("alice").size());
    }

    @Test
    void theDayIsSeededAndContainsTheAttack() {
        assertEquals(Sources.day(1).web().size(), Sources.day(1).web().size());
        assertTrue(Sources.day(1).db().stream().anyMatch(l -> l.contains("actor=alice") && l.contains("rows=500000")));
        assertEquals(20, Sources.day(1).db().stream().filter(l -> l.startsWith("time=garbled")).count());
    }
}
