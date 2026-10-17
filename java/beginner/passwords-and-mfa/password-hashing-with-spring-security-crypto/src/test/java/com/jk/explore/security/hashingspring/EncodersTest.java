package com.jk.explore.security.hashingspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

class EncodersTest {

    @Test
    void theDelegatingEncoderPrefixesTheAlgorithmAndSaltsEveryHash() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String first = encoder.encode("welcome1");
        String second = encoder.encode("welcome1");
        assertTrue(first.startsWith("{bcrypt}"));
        assertEquals(68, first.length());
        assertNotEquals(first, second);
        assertTrue(encoder.matches("welcome1", first));
        assertFalse(encoder.matches("guess", first));
    }

    @Test
    void everyAlgorithmVerifiesItsOwnHashAndRejectsAWrongPassword() {
        PasswordEncoder[] encoders = {
            new BCryptPasswordEncoder(4),
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8(),
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        };
        for (PasswordEncoder encoder : encoders) {
            String stored = encoder.encode("welcome1");
            assertTrue(encoder.matches("welcome1", stored), encoder.getClass().getSimpleName());
            assertFalse(encoder.matches("welcome2", stored), encoder.getClass().getSimpleName());
            assertNotEquals(stored, encoder.encode("welcome1"), "each hash has its own salt");
        }
    }

    @Test
    void bcryptShowsItsCostInTheStoredText() {
        String stored = new BCryptPasswordEncoder(6).encode("x");
        assertEquals("$2a$06$", stored.substring(0, 7));
    }

    @Test
    void argon2ShowsItsSettingsInTheStoredText() {
        String stored = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("x");
        assertEquals("m=16384,t=2,p=1", stored.split("\\$")[3]);
    }

    @Test
    void movingToAnotherAlgorithmKeepsOldHashesWorkingAndFlagsThemForUpgrade() {
        DelegatingPasswordEncoder old = SpringHashingDemo.allowList("bcrypt");
        DelegatingPasswordEncoder current = SpringHashingDemo.allowList(SpringHashingDemo.ARGON2_ID);
        String stored = old.encode("letmein");
        assertTrue(current.matches("letmein", stored));
        assertTrue(current.upgradeEncoding(stored));
        String upgraded = current.encode("letmein");
        assertTrue(upgraded.startsWith("{" + SpringHashingDemo.ARGON2_ID + "}"));
        assertFalse(current.upgradeEncoding(upgraded));
        assertTrue(current.matches("letmein", upgraded));
    }

    @Test
    void theDefaultDelegatingEncoderAcceptsANoopValueButAnAllowListDoesNot() {
        assertTrue(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("letmein", "{noop}letmein"));
        DelegatingPasswordEncoder strict = SpringHashingDemo.allowList(SpringHashingDemo.ARGON2_ID);
        assertThrows(IllegalArgumentException.class, () -> strict.matches("letmein", "{noop}letmein"));
    }

    @Test
    void thePrefixIsTheTextUpToTheClosingBrace() {
        assertEquals("{bcrypt}", SpringHashingDemo.prefix("{bcrypt}$2a$10$abc"));
    }
}
