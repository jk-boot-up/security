package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
