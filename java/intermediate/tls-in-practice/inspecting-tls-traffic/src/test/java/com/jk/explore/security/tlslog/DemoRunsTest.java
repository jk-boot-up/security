package com.jk.explore.security.tlslog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Keys come from a labelled generator and the RSA key is never printed, so the text is the same every run. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), TlsLogDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(TlsLogDemo.run(), TlsLogDemo.run());
    }
}
