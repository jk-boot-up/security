package com.jk.explore.security.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretTest {

    @Test
    void neverPrintsItsValue() {
        Secret secret = Secret.of("demo-value");
        assertEquals("[redacted]", secret.toString());
        assertEquals("password [redacted]", "password " + secret);
    }

    @Test
    void comparesValuesNotStrings() {
        Secret secret = Secret.of("demo-value");
        assertTrue(secret.matches("demo-value".toCharArray()));
        assertFalse(secret.matches("demo-valuE".toCharArray()));
        assertFalse(secret.matches("demo".toCharArray()));
        assertTrue(secret.matches(Secret.of("demo-value")));
    }

    @Test
    void wipingClearsTheArrayAndStopsUse() {
        char[] value = "demo-value".toCharArray();
        Secret secret = Secret.of(value);
        secret.wipe();
        for (char c : value) {
            assertEquals('\0', c);
        }
        assertTrue(secret.isWiped());
        assertFalse(secret.matches("demo-value".toCharArray()));
        assertThrows(IllegalStateException.class, () -> secret.use(chars -> chars.length));
    }
}
