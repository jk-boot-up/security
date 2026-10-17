package com.jk.explore.security.backupsminio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo starts a real object store in Docker, so this test runs with ./gradlew integrationTest. */
@Tag("docker")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), BackupsMinioDemo.run());
    }
}
