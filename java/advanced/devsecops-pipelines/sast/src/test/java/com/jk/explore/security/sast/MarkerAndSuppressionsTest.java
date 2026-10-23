package com.jk.explore.security.sast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkerAndSuppressionsTest {

    @Test
    void theCorpusMarksSevenFlawsAndSevenSafeLines() throws Exception {
        List<Marker> markers = Marker.in(Path.of("corpus"));
        assertEquals(7, markers.stream().filter(Marker::vulnerable).count());
        assertEquals(7, markers.stream().filter(m -> !m.vulnerable()).count());
    }

    @Test
    void everyMarkerLineHoldsAStatementNotJustAComment() throws Exception {
        for (Marker m : Marker.in(Path.of("corpus"))) {
            String line = Files.readAllLines(Path.of("corpus/bank", m.file())).get(m.line() - 1).strip();
            assertTrue(!line.startsWith("//"), m.key());
        }
    }

    @Test
    void aSuppressionNeedsTheRuleAReasonAndAnOwner() throws Exception {
        Path dir = Files.createTempDirectory("s");
        Files.writeString(dir.resolve("A.java"), "// nosemgrep: r.x -- checked against an allowlist above (owner: team-a)\n// nosemgrep: r.x\n// nosemgrep: r.x -- short (owner: a)\n");
        assertEquals(new Suppressions.Count(3, 1), Suppressions.count(dir));
    }

    @Test
    void aCommentIsInsertedBeforeTheLineAndTheRestMovesDown() {
        assertEquals(List.of("a", "// c", "b"), Suppressions.insertBefore(List.of("a", "b"), 2, "// c"));
    }

    @Test
    void theRuleFilesHaveTheirIds() throws Exception {
        String taint = Files.readString(Path.of("rules/taint.yml"));
        for (String id : List.of("sql-taint", "command-taint", "path-taint", "weak-hash")) {
            assertTrue(taint.contains("id: " + id), id);
        }
    }
}
