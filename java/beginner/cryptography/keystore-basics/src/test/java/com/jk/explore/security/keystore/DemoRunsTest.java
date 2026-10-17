package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Keys and salts are random, so it prints only facts that do not depend on them. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws IOException {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), KeystoreDemo.run());
    }

    @Test
    void demoIsRepeatable() throws IOException {
        assertEquals(KeystoreDemo.run(), KeystoreDemo.run());
    }
}
