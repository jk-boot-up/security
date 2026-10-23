package com.jk.explore.security.sast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreTest {

    private static final List<Marker> MARKERS = List.of(
            new Marker("A.java", 10, true, "sql-direct"),
            new Marker("A.java", 20, true, "sql-other"),
            new Marker("A.java", 30, false, "sql-prepared"),
            new Marker("B.java", 5, true, "command"));

    @Test
    void aFindingOnAFlawLineIsFoundAndTheOtherIsMissed() {
        Score s = Score.of(MARKERS, List.of(new Finding("r", "A.java", 10)), "sql-");
        assertEquals(List.of("sql-direct"), s.found().stream().map(Marker::id).toList());
        assertEquals(List.of("sql-other"), s.missed().stream().map(Marker::id).toList());
    }

    @Test
    void aFindingOnASafeLineIsFlagged() {
        Score s = Score.of(MARKERS, List.of(new Finding("r", "A.java", 30)), "sql-");
        assertEquals(1, s.flaggedSafe().size());
        assertEquals("0 of 2 flaws found, 1 of 1 safe lines flagged", s.line());
    }

    @Test
    void theSameLineNumberInAnotherFileIsAnotherLine() {
        Score s = Score.of(MARKERS, List.of(new Finding("r", "B.java", 10)), "sql-");
        assertEquals(0, s.found().size());
        assertEquals(1, s.otherFindings());
    }

    @Test
    void thePrefixSelectsAClassOfFlaw() {
        assertEquals(1, Score.of(MARKERS, List.of(new Finding("r", "B.java", 5)), "command").found().size());
    }

    @Test
    void namesOfNoneReadNone() {
        assertEquals("none", Score.names(List.of()));
    }
}
