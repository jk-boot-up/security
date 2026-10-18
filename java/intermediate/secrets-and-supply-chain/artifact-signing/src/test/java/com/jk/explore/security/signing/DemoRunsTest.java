package com.jk.explore.security.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Ed25519 is deterministic and the keys come from fixed seeds, so it does. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), SigningDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(SigningDemo.run(), SigningDemo.run());
    }
}
