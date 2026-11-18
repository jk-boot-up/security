package com.jk.explore.security.oidcspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the golden output without Docker: five acts, and every line short enough for an output slide. */
class GoldenOutputTest {

    @Test
    void theGoldenOutputHasFiveActsAndShortLines() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("docs", "expected-output.txt"));
        assertEquals(5, lines.stream().filter(l -> l.matches("^(ONE|TWO|THREE|FOUR|FIVE)\\. .*")).count());
        for (String line : lines) {
            assertTrue(line.length() <= 70, line);
        }
    }

    @Test
    void keycloakAddressesArePlaceholders() {
        assertEquals("<keycloak>/realms/bank", OidcSpringDemo.local("http://127.0.0.1:41234/realms/bank"));
    }
}
