package com.jk.explore.security.threatmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), ThreatModelDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(ThreatModelDemo.run(), ThreatModelDemo.run());
    }
}
