package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo needs only the JDK and Bouncy Castle and runs in seconds, so it runs with the ordinary tests. It must print exactly the golden output. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), PostQuantumDemo.run());
    }
}
