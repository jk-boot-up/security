package com.jk.explore.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), OidcDemo.run());
    }

    @Test
    void everyLineFitsOnAnOutputSlide() throws Exception {
        for (String line : OidcDemo.run().split("\n")) {
            assertTrue(line.length() <= 70, line);
        }
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(OidcDemo.run(), OidcDemo.run());
    }
}
