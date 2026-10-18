package com.jk.explore.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PkceTest {

    @Test
    void s256MatchesTheRfc7636AppendixBExample() {
        assertEquals(OAuthDemo.RFC7636_CHALLENGE, Pkce.challenge(OAuthDemo.RFC7636_VERIFIER));
        assertTrue(Pkce.matches(OAuthDemo.RFC7636_CHALLENGE, OAuthDemo.RFC7636_VERIFIER));
    }

    @Test
    void anotherVerifierDoesNotMatch() {
        assertFalse(Pkce.matches(OAuthDemo.RFC7636_CHALLENGE, Pkce.newVerifier()));
        assertFalse(Pkce.matches(OAuthDemo.RFC7636_CHALLENGE, null));
    }

    @Test
    void verifiersAreFreshAndWellFormed() {
        String a = Pkce.newVerifier();
        assertEquals(43, a.length());
        assertTrue(Pkce.wellFormed(a));
        assertNotEquals(a, Pkce.newVerifier());
    }

    @Test
    void badlyFormedVerifiersAreRefused() {
        assertFalse(Pkce.wellFormed("short"));
        assertFalse(Pkce.wellFormed("a".repeat(129)));
        assertFalse(Pkce.wellFormed("a".repeat(42) + "!"));
        assertTrue(Pkce.wellFormed("a".repeat(40) + "-._~"));
        assertFalse(Pkce.matches(Pkce.challenge("short"), "short"));
    }
}
