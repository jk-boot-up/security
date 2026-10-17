package com.jk.explore.security.httpsspring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. It starts real servers on free ports, and prints only facts that do not depend on the ports. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), HttpsSpringDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(HttpsSpringDemo.run(), HttpsSpringDemo.run());
    }
}
