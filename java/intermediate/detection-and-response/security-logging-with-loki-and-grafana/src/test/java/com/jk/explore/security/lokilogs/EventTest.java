package com.jk.explore.security.lokilogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The events, with no server. */
class EventTest {

    @Test
    void theMorningHasEightEventsOfFourTypes() {
        List<Event> morning = Event.morning();
        assertEquals(8, morning.size());
        assertEquals(4, morning.stream().map(Event::eventType).distinct().count());
    }

    @Test
    void aLineIsJsonWithFixedFieldsAndTheAmountOnlyWhenThereIsOne() {
        assertEquals("{\"request_id\":\"req-0001\",\"event_type\":\"login\",\"actor\":\"ana\",\"action\":\"login\",\"resource\":\"session\",\"source_ip\":\"203.0.113.5\",\"outcome\":\"success\"}", Event.morning().get(0).line());
        assertTrue(Event.morning().get(5).line().endsWith("\"amount\":500}"));
        assertFalse(Event.morning().get(0).line().contains("amount"));
    }

    @Test
    void textWithALineBreakStaysOnOneLine() {
        Event forged = new Event("req-9", "login", "mallory\n2026-10-18T09:09:00Z INFO login ok user=admin", "login", "session", "203.0.113.1", "failure", null);
        assertEquals(1, forged.line().lines().count());
        assertTrue(forged.line().contains("\\n"));
    }
}
