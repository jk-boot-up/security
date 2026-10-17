package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo starts real nginx containers, so this needs a running Docker: run it with ./gradlew integrationTest. */
@Tag("docker")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), HttpsNginxDemo.run());
    }
}
