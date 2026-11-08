package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CredentialsTest {

    private final Credentials credentials = new Credentials().add("ben", "ben-pass");

    @Test
    void theRightPasswordIsAccepted() {
        assertTrue(credentials.verify("ben", "ben-pass"));
    }

    @Test
    void aWrongPasswordIsRejected() {
        assertFalse(credentials.verify("ben", "guess"));
    }

    @Test
    void anUnknownUserIsRejected() {
        assertFalse(credentials.verify("nobody", "ben-pass"));
    }

    @Test
    void theSamePasswordForAnotherUserDoesNotMatch() {
        Credentials two = new Credentials().add("ben", "shared").add("ana", "shared");
        assertTrue(two.verify("ana", "shared"));
        assertFalse(new Credentials().add("ben", "shared").verify("ana", "shared"));
    }

    @Test
    void onlyTheSaltedHashIsKeptAndItIs32Bytes() {
        byte[] stored = credentials.storedHash("ben");
        assertEquals(32, stored.length);
        assertArrayEquals(Credentials.hash("ben", "ben-pass"), stored);
        assertNull(credentials.storedHash("nobody"));
    }

    @Test
    void theSamePasswordWithAnotherSaltGivesAnotherHash() {
        assertFalse(Arrays.equals(Credentials.hash("ben", "shared"), Credentials.hash("ana", "shared")));
    }
}
