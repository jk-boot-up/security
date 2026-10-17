package com.jk.explore.security.attacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output, which the docs, the animation and the video quote. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws IOException {
        String expected = Files.readString(Path.of("docs", "expected-output.txt"));
        assertEquals(expected, AttacksDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(AttacksDemo.run(), AttacksDemo.run());
    }
}
