package com.jk.explore.security.pytmmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo, printed from the recorded pytm answers, must be exactly the golden output. Needs no Python. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), PytmDemo.run(new Recorded(Recorded.DIR)));
    }
}
