package com.jk.explore.security.totp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class AuthenticatorTest {

    @Test
    void aPhoneOnTimeShowsTheCodeOfTheCurrentStep() {
        assertEquals("287082", new Authenticator(DemoSecrets.RFC_KEY, 0).codeShownAt(59));
    }

    @Test
    void aFastPhoneShowsTheCodeOfALaterStep() {
        assertEquals("359152", new Authenticator(DemoSecrets.RFC_KEY, 25).codeShownAt(59));
        assertEquals("338314", new Authenticator(DemoSecrets.RFC_KEY, 70).codeShownAt(59));
    }

    @Test
    void aPhoneWithADifferentSecretShowsADifferentCode() {
        assertNotEquals(new Authenticator(DemoSecrets.RFC_KEY, 0).codeShownAt(59),
                new Authenticator("another secret value".getBytes(), 0).codeShownAt(59));
    }
}
