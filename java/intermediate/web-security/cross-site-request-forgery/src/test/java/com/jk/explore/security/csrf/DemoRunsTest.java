package com.jk.explore.security.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Tokens and session ids are random, so it prints only facts that do not depend on them. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), CsrfDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(CsrfDemo.run(), CsrfDemo.run());
    }
}
