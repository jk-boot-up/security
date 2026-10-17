package com.jk.explore.security.hashingspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeakEncodersTest {

    @Test
    void thePlainTextEncoderStoresThePassword() {
        PlainTextEncoder encoder = new PlainTextEncoder();
        assertEquals("welcome1", encoder.encode("welcome1"));
        assertTrue(encoder.matches("welcome1", "welcome1"));
        assertFalse(encoder.matches("guess", "welcome1"));
    }

    @Test
    void theFastSaltedEncoderGivesDifferentValuesButStillMatches() {
        FastSaltedSha256Encoder encoder = new FastSaltedSha256Encoder();
        String a = encoder.encode("welcome1");
        String b = encoder.encode("welcome1");
        assertNotEquals(a, b);
        assertTrue(encoder.matches("welcome1", a));
        assertFalse(encoder.matches("welcome2", a));
    }

    @Test
    void theFastSaltedEncoderFallsToTheSameAttackInTwentyFourGuesses() {
        FastSaltedSha256Encoder encoder = new FastSaltedSha256Encoder();
        java.util.Map<String, String> table = new java.util.LinkedHashMap<>();
        Customers.PASSWORDS.forEach((user, password) -> table.put(user, encoder.encode(password)));
        Attacker.Cracked cracked = new Attacker(Customers.WORDLIST).crack(table, encoder);
        assertEquals(24, cracked.guesses());
        assertEquals(4, cracked.found().size());
    }
}
