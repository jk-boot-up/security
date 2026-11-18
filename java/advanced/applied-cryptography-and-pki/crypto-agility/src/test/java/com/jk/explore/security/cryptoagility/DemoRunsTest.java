package com.jk.explore.security.cryptoagility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs only the JDK. Keys and ciphertext are never printed, only counts and lengths. */

class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), CryptoAgilityDemo.run());
    }
}
