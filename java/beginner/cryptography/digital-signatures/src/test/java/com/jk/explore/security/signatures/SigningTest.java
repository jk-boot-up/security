package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class SigningTest {

    private final byte[] message = Hex.bytes("laptop=1200");

    @Test
    void aSignatureIs64BytesAndVerifies() {
        KeyPair keys = Ed25519Keys.newKeyPair();
        byte[] signature = Signing.sign(keys.getPrivate(), "ctx", message);
        assertEquals(64, signature.length);
        assertTrue(Signing.verify(keys.getPublic(), "ctx", message, signature));
    }

    @Test
    void theSameKeyAndMessageGiveTheSameSignature() {
        KeyPair keys = Ed25519Keys.newKeyPair();
        assertArrayEquals(Signing.sign(keys.getPrivate(), "ctx", message), Signing.sign(keys.getPrivate(), "ctx", message));
    }

    @Test
    void aDifferentKeyDoesNotVerify() {
        KeyPair a = Ed25519Keys.newKeyPair();
        KeyPair b = Ed25519Keys.newKeyPair();
        assertFalse(Signing.verify(b.getPublic(), "ctx", message, Signing.sign(a.getPrivate(), "ctx", message)));
    }

    @Test
    void aDifferentContextDoesNotVerify() {
        KeyPair keys = Ed25519Keys.newKeyPair();
        byte[] signature = Signing.sign(keys.getPrivate(), "shop.refund.v1", message);
        assertFalse(Signing.verify(keys.getPublic(), "shop.price-list.v1", message, signature));
        assertTrue(Signing.verify(keys.getPublic(), "shop.refund.v1", message, signature));
    }

    @Test
    void theContextCannotRunIntoTheMessage() {
        // "ab" + 0 + "c" and "a" + 0 + "bc" are different byte strings, so a boundary cannot move
        assertFalse(java.util.Arrays.equals(Signing.withContext("ab", Hex.bytes("c")), Signing.withContext("a", Hex.bytes("bc"))));
    }

    @Test
    void theSignatureHidesNothingAboutTheMessage() {
        // the signature is separate from the message, which stays readable
        KeyPair keys = Ed25519Keys.newKeyPair();
        byte[] signature = Signing.sign(keys.getPrivate(), "ctx", message);
        assertEquals("laptop=1200", new String(message, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(64, signature.length);
    }
}
