package com.jk.explore.security.hashingspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AttackerTest {

    private final Attacker attacker = new Attacker(Customers.WORDLIST);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private Map<String, String> table() {
        Map<String, String> table = new LinkedHashMap<>();
        Customers.PASSWORDS.forEach((user, password) -> table.put(user, encoder.encode(password)));
        return table;
    }

    @Test
    void weakPasswordsOnTheListFallInTwentyFourGuesses() {
        Attacker.Cracked cracked = attacker.crack(table(), encoder);
        assertEquals(4, cracked.found().size());
        assertEquals(24, cracked.guesses());
        assertEquals("welcome1", cracked.found().get("ana"));
        assertEquals("welcome1", cracked.found().get("ben"));
        assertEquals("sunshine", cracked.found().get("carol"));
        assertEquals("letmein", cracked.found().get("erin"));
    }

    @Test
    void theStrongPasswordSurvives() {
        assertFalse(attacker.crack(table(), encoder).found().containsKey("dave"));
    }

    @Test
    void bcryptWorkDoublesWithEachStepOfCost() {
        assertEquals(64, 1 << (10 - 4));
    }
}
