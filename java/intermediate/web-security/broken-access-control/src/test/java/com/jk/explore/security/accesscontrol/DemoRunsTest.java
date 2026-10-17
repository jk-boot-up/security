package com.jk.explore.security.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output, which the docs, the animation and the video quote. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), AccessControlDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(AccessControlDemo.run(), AccessControlDemo.run());
    }
}
