package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Ed25519 is deterministic and the clock is simulated, so every run is the same. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), JwtDemo.run());
    }

    @Test
    void demoIsRepeatable() {
        assertEquals(JwtDemo.run(), JwtDemo.run());
    }
}
