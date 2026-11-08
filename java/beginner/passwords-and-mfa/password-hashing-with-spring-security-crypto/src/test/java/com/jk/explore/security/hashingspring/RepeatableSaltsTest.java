package com.jk.explore.security.hashingspring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class RepeatableSaltsTest {

    @Test
    void theSameSeedGivesTheSameSaltsInTheSameOrder() {
        byte[] a = new byte[16];
        byte[] b = new byte[16];
        RepeatableSalts first = new RepeatableSalts();
        RepeatableSalts second = new RepeatableSalts();
        first.nextBytes(a);
        second.nextBytes(b);
        assertArrayEquals(a, b);
        byte[] next = new byte[16];
        first.nextBytes(next);
        assertFalse(java.util.Arrays.equals(a, next), "each salt is new");
    }

    @Test
    void aRepeatableBcryptTextIsAnOrdinaryOneThatTheLibraryDefaultReads() {
        BCryptPasswordEncoder.BCryptVersion v = BCryptPasswordEncoder.BCryptVersion.$2A;
        String one = new BCryptPasswordEncoder(v, 10, new RepeatableSalts()).encode("welcome1");
        String again = new BCryptPasswordEncoder(v, 10, new RepeatableSalts()).encode("welcome1");
        assertEquals(one, again);
        String fresh = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("welcome1");
        assertEquals(fresh.length(), ("{bcrypt}" + one).length());
        assertTrue(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("welcome1", "{bcrypt}" + one));
        assertNotEquals(fresh, "{bcrypt}" + one);
    }

    @Test
    void theSettingsAreTheTextBeforeTheSalt() {
        assertEquals("$2a$10$", SpringHashingDemo.settings("$2a$10$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvwxyz12345"));
        assertEquals("$argon2id$v=19$m=16384,t=2,p=1$", SpringHashingDemo.settings("$argon2id$v=19$m=16384,t=2,p=1$salt$hash"));
        assertEquals("salt + hash in hex", SpringHashingDemo.settings("abcdef0123"));
    }
}
