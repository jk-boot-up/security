package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Pbkdf2StepsTest {

    @Test
    void oneRoundIsJustU1AndMatchesThePublishedValue() {
        Pbkdf2Steps.Steps steps = Pbkdf2Steps.derive("password", Hex.bytes("salt"), 1);
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", Hex.of(steps.key()));
        assertArrayEquals(steps.u1(), steps.key());
    }

    @Test
    void twoRoundsAreU1XorU2() {
        Pbkdf2Steps.Steps steps = Pbkdf2Steps.derive("password", Hex.bytes("salt"), 2);
        byte[] xor = new byte[32];
        for (int i = 0; i < 32; i++) {
            xor[i] = (byte) (steps.u1()[i] ^ steps.u2()[i]);
        }
        assertArrayEquals(xor, steps.key());
        assertEquals("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43", Hex.of(steps.key()));
    }

    @Test
    void byHandGivesTheJdksKeyForTheDemosPasswordSaltAndRounds() {
        byte[] salt = Hex.bytes("per-user-salt-1");
        assertArrayEquals(Derivation.fromPassword(KeystoreDemo.PASSWORD, salt, Derivation.PBKDF2_ROUNDS),
                Pbkdf2Steps.derive(new String(KeystoreDemo.PASSWORD), salt, Derivation.PBKDF2_ROUNDS).key());
    }
}
