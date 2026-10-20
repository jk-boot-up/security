package com.jk.explore.security.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Tokens and keys are random, so it prints only statuses and messages. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), TokensDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(TokensDemo.run(), TokensDemo.run());
    }
}
