package com.jk.explore.security.hashingspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class BcryptStepsTest {

    private final PasswordEncoder library = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void theStoredTextSplitsIntoIdVersionCostSaltAndHash() {
        String stored = library.encode("welcome1");
        BcryptSteps.Parts p = BcryptSteps.parts(stored);
        assertEquals("{bcrypt}", p.id());
        assertEquals("$2a$", p.version());
        assertEquals(10, p.cost());
        assertEquals(1024, p.rounds());
        assertEquals(22, p.salt().length());
        assertEquals(31, p.hash().length());
        assertEquals(stored, p.id() + p.version() + p.cost() + "$" + p.salt() + p.hash());
    }

    @Test
    void theLoginCheckByHandGivesTheLibrarysAnswer() {
        String stored = library.encode("welcome1");
        BcryptSteps.Parts p = BcryptSteps.parts(stored);
        for (String typed : new String[] {"welcome1", "guess", "welcome2"}) {
            assertEquals(library.matches(typed, stored), BcryptSteps.sameHash(BcryptSteps.hashAgain(typed, p), p), typed);
        }
        assertTrue(BcryptSteps.sameHash(BcryptSteps.hashAgain("welcome1", p), p));
        assertFalse(BcryptSteps.sameHash(BcryptSteps.hashAgain("guess", p), p));
    }
}
