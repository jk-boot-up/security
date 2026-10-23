package com.jk.explore.security.mtls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs step and step-ca on the PATH, and takes about 25 seconds (a certificate that lives 8 seconds is allowed to expire). Ports, serial numbers and paths are never printed. */
@Tag("integration")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), MutualTlsDemo.run());
    }
}
