package com.jk.explore.security.lokilogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The stream counts the demo works out by hand, with no server. */
class StreamsTest {

    @Test
    void theMorningMakesOneStreamPerEventType() {
        assertEquals(4, Streams.count(Event.morning().stream().map(LokiLogsDemo::labelsOf).toList()));
    }

    @Test
    void aUserAsALabelMakesOneStreamPerUserAndInTheLineOnlyOne() {
        List<Map<String, String>> labelled = new ArrayList<>();
        List<Map<String, String>> content = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            labelled.add(Map.of("app", "bank-labelled", "event_type", "login", "actor", "user" + i));
            content.add(Map.of("app", "bank-content", "event_type", "login"));
        }
        assertEquals(150, Streams.count(labelled));
        assertEquals(1, Streams.count(content));
    }

    @Test
    void wrapCutsAfterCommasAndKeepsEveryCharacter() {
        String line = Event.morning().get(5).line();
        List<String> pieces = Streams.wrap(line, 58);
        assertEquals(line, String.join("", pieces));
        assertTrue(pieces.stream().allMatch(p -> p.length() <= 58));
    }
}
