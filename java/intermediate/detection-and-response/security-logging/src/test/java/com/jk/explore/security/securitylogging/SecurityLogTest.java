package com.jk.explore.security.securitylogging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityLogTest {

    @TempDir
    Path dir;

    @Test
    void noSecretFromTheMorningReachesTheLog() throws Exception {
        List<String> log = SecurityLoggingDemo.securityLog();
        for (String secret : BankDay.SECRETS) {
            assertTrue(log.stream().noneMatch(l -> l.contains(secret)), secret);
        }
    }

    @Test
    void everyLineHasTheSameFieldsAndARequestId() throws Exception {
        for (String line : SecurityLoggingDemo.securityLog()) {
            Map<String, String> f = Json.parse(line);
            for (String field : List.of("prev", "time", "type", "actor", "action", "target", "source", "outcome", "correlation")) {
                assertTrue(f.containsKey(field), field);
            }
            assertTrue(f.get("correlation").startsWith("req-"));
        }
    }

    @Test
    void theOldLoggerCannotSayWhoMovedTheMoneyAndTheNewOneCan() throws Exception {
        assertEquals(0, LogQuery.plainLinesMentioning(SecurityLoggingDemo.oldLog(), "ANA-1"));
        Map<String, String> transfer = LogQuery.where(SecurityLoggingDemo.securityLog(), "target", "ANA-1 -> MAL-9").getFirst();
        assertEquals("ana", transfer.get("actor"));
        assertEquals(BankDay.STOLEN_FROM, transfer.get("source"));
    }

    @Test
    void aLineBreakInUserTextForgesALineInPlainTextOnly() throws Exception {
        SimClock clock = new SimClock();
        PlainLogger plain = new PlainLogger(clock);
        plain.error("login failed user=x\nforged");
        assertEquals(2, plain.lines().size());
        MemorySink sink = new MemorySink();
        new SecurityLog(sink).record(new SecurityEvent(clock.now(), EventType.LOGIN, "x\nforged", "login", "x", "192.0.2.1",
                Outcome.FAILURE, "req-1", Map.of()));
        assertEquals(1, sink.lines().size());
        assertEquals("x\nforged", Json.parse(sink.lines().getFirst()).get("actor"));
    }

    @Test
    void theChainFindsDeletedAndEditedLines() throws Exception {
        List<String> lines = SecurityLoggingDemo.securityLog();
        assertTrue(ChainCheck.verify(lines, SecurityLog.GENESIS).intact());
        List<String> deleted = new ArrayList<>(lines);
        deleted.remove(2);
        assertEquals(3, ChainCheck.verify(deleted, SecurityLog.GENESIS).brokenAt());
        List<String> edited = new ArrayList<>(lines);
        edited.set(2, lines.get(2).replace("ana", "bob"));
        assertEquals(4, ChainCheck.verify(edited, SecurityLog.GENESIS).brokenAt());
        List<String> garbled = new ArrayList<>(lines);
        garbled.set(0, "not json");
        assertEquals(1, ChainCheck.verify(garbled, SecurityLog.GENESIS).brokenAt());
    }

    @Test
    void theLastLineNeedsThePublishedHead() throws Exception {
        MemorySink sink = new MemorySink();
        SecurityLog log = new SecurityLog(sink);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        List<String> lines = new ArrayList<>(sink.lines());
        assertEquals(log.head(), ChainCheck.verify(lines, SecurityLog.GENESIS).head());
        lines.set(lines.size() - 1, lines.getLast().replace("BOOKS-4", "BOOKS-5"));
        ChainCheck.Result result = ChainCheck.verify(lines, SecurityLog.GENESIS);
        assertTrue(result.intact());
        assertNotEquals(log.head(), result.head());
        List<String> truncated = sink.lines().subList(0, 7);
        assertNotEquals(log.head(), ChainCheck.verify(truncated, SecurityLog.GENESIS).head());
    }

    @Test
    void theFileIsOwnerOnlyAndAppendKeepsEveryLine() throws Exception {
        FileSink file = new FileSink(dir.resolve("security.log"));
        SecurityLog log = new SecurityLog(file);
        for (SecurityEvent event : BankDay.events(new SimClock())) {
            log.record(event);
        }
        assertEquals("rw-------", file.permissions());
        assertEquals(8, file.lines().size());
        assertTrue(ChainCheck.verify(file.lines(), SecurityLog.GENESIS).intact());
    }

    @Test
    void retentionKeepsAChainThatStillChecksFromItsAnchor() throws Exception {
        MemorySink sink = new MemorySink();
        SecurityLog log = new SecurityLog(sink);
        SimClock clock = new SimClock(Instant.parse("2026-01-01T00:00:00Z"));
        for (int day = 0; day < 10; day++) {
            log.record(new SecurityEvent(clock.now(), EventType.LOGIN, "c" + day, "login", "c" + day, "192.0.2.1", Outcome.SUCCESS,
                    "req-" + day, Map.of()));
            clock.advance(Duration.ofDays(1));
        }
        Retention.Pruned pruned = Retention.prune(sink.lines(), clock.now(), Duration.ofDays(4));
        assertEquals(6, pruned.removed());
        assertEquals(4, pruned.kept().size());
        assertTrue(ChainCheck.verify(pruned.kept(), pruned.anchor()).intact());
        assertFalse(ChainCheck.verify(pruned.kept(), SecurityLog.GENESIS).intact());
    }
}
