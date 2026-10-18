package com.jk.explore.security.bff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Session ids, refresh tokens and proof ids are random, so none is printed. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), BffDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(BffDemo.run(), BffDemo.run());
    }
}
