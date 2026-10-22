package com.jk.explore.security.hostscan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs Docker; container names are never printed. */
@Tag("integration")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), HostScanDemo.run());
    }
}
