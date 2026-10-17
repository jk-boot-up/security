package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticatorTest {

    private final AuthDemo.World w = new AuthDemo.World();

    @Test
    void aCorrectPasswordGivesANumberedToken() {
        assertEquals("tok-1", w.authenticator.login("ben", "ben-pass").orElseThrow().token());
        assertEquals("tok-2", w.authenticator.login("ana", "ana-pass").orElseThrow().token());
    }

    @Test
    void aWrongPasswordGivesNothing() {
        assertTrue(w.authenticator.login("ben", "guess").isEmpty());
    }

    @Test
    void aLiveTokenIsRecognised() {
        String token = w.login("ben");
        assertEquals("ben", w.authenticator.authenticate(token).orElseThrow().username());
    }

    @Test
    void unknownAndMissingTokensAreNotRecognised() {
        assertTrue(w.authenticator.authenticate("tok-99").isEmpty());
        assertTrue(w.authenticator.authenticate(null).isEmpty());
    }

    @Test
    void aTokenExpiresAfterItsLifetime() {
        String token = w.login("ben");
        w.ticker.advance(AuthDemo.SESSION_LIFETIME - 1);
        assertTrue(w.authenticator.authenticate(token).isPresent());
        w.ticker.advance(1);
        assertFalse(w.authenticator.authenticate(token).isPresent());
    }

    @Test
    void logoutEndsTheSessionEarly() {
        String token = w.login("ben");
        w.authenticator.logout(token);
        assertTrue(w.authenticator.authenticate(token).isEmpty());
    }
}
