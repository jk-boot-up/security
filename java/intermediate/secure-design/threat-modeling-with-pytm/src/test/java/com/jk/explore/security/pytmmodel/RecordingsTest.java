package com.jk.explore.security.pytmmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The demo against the real pytm prints the golden output, and every recording in src/test/resources/recorded is what pytm answers today.
 * To record again: run this test and copy build/recorded over src/test/resources/recorded.
 */
@Tag("integration")
class RecordingsTest {

    @Test
    void theRealPytmPrintsTheGoldenOutputAndMatchesTheRecordings() throws Exception {
        Path fresh = Path.of("build", "recorded");
        String output = PytmDemo.run(new Recording(new Pytm(Path.of("build")), fresh));
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), output);
        List<Path> files;
        try (Stream<Path> s = Files.list(fresh)) {
            files = s.sorted().toList();
        }
        try (Stream<Path> s = Files.list(Recorded.DIR)) {
            assertEquals(files.size(), s.count(), "the number of recorded files");
        }
        for (Path f : files) {
            assertEquals(Files.readString(f), Files.readString(Recorded.DIR.resolve(f.getFileName())), f.getFileName().toString());
        }
    }
}
