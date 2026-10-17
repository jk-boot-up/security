package com.jk.explore.security.tlsopenssl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. It uses real sockets but prints no ports or random values. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), TlsOpenSslDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(TlsOpenSslDemo.run(), TlsOpenSslDemo.run());
    }
}
