package com.jk.explore.security.authspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Hashing by hand gives exactly the line BCrypt stores, and Spring's own encoder accepts it. */
class HashStepsTest {

    private final HashSteps.Parts stored = HashSteps.hash("ben-pass", SpringAuthDemo.SALT, 4);

    @Test
    void theStoredLineHasFourParts() {
        assertEquals("$2a$", stored.version());
        assertEquals("04", stored.cost());
        assertEquals(SpringAuthDemo.SALT, stored.salt());
        assertEquals(31, stored.hash().length());
        assertEquals(60, stored.stored().length());
    }

    @Test
    void theSameSaltAndPasswordGiveTheSameHash() {
        assertEquals(stored.hash(), HashSteps.again("ben-pass", stored).hash());
        assertNotEquals(stored.hash(), HashSteps.again("guess", stored).hash());
    }

    @Test
    void springsEncoderAcceptsTheLineMadeByHand() {
        BCryptPasswordEncoder spring = new BCryptPasswordEncoder(4);
        assertTrue(spring.matches("ben-pass", stored.stored()));
        assertFalse(spring.matches("guess", stored.stored()));
    }

    @Test
    void aRandomSaltGivesADifferentLineEveryTime() {
        BCryptPasswordEncoder spring = new BCryptPasswordEncoder(4);
        assertNotEquals(spring.encode("ben-pass"), spring.encode("ben-pass"));
    }
}
