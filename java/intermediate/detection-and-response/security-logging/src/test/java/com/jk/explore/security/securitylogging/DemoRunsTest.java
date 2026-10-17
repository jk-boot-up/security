package com.jk.explore.security.securitylogging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), SecurityLoggingDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(SecurityLoggingDemo.run(), SecurityLoggingDemo.run());
    }
}
