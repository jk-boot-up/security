package com.jk.explore.security.tlsattacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs OpenSSL 3 or 4, git and network access once (testssl.sh is cloned into build/). Ports and keys are never printed. */
@Tag("integration")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), TlsAttacksDemo.run(Path.of("build")));
    }
}
