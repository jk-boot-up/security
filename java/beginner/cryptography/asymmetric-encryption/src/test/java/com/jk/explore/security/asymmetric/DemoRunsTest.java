package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Keys are random, so the demo prints only facts that do not depend on them. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws IOException {
        String expected = Files.readString(Path.of("docs", "expected-output.txt"));
        assertEquals(expected, AsymmetricDemo.run());
    }

    @Test
    void demoIsRepeatableDespiteRandomKeys() {
        assertEquals(AsymmetricDemo.run(), AsymmetricDemo.run());
    }
}
