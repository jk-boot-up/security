package com.jk.explore.security.privacyeng;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs Docker and the postgres:18.6-alpine image. Cluster, registry and image names are never printed. */
@Tag("integration")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), PrivacyEngineeringDemo.run());
    }
}
