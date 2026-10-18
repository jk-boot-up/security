package com.jk.explore.security.races;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output, on every run. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), RacesDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(RacesDemo.run(), RacesDemo.run());
    }
}
