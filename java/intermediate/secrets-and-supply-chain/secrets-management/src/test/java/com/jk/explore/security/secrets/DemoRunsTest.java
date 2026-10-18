package com.jk.explore.security.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output, and never a secret value. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), SecretsDemo.run());
    }

    @Test
    void demoIsRepeatable() throws Exception {
        assertEquals(SecretsDemo.run(), SecretsDemo.run());
    }

    @Test
    void demoNeverPrintsASecretValue() throws Exception {
        String output = SecretsDemo.run();
        assertFalse(output.contains(SecretsDemo.DEMO_DB_PASSWORD));
        assertFalse(output.contains(SecretsDemo.DEMO_PAYMENT_KEY));
        assertFalse(output.contains(SecretsDemo.randomLookingToken()));
    }
}
