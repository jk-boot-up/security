package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.KeyPair;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class KeyAgreementTest {

    @Test
    void bothSidesDeriveTheSameKey() {
        KeyPair bank = KeyAgreementBox.newKeyPair();
        KeyPair ana = KeyAgreementBox.newKeyPair();
        byte[] salt = Hex.bytes("s");
        byte[] info = Hex.bytes("i");
        assertArrayEquals(
                KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(bank.getPrivate(), ana.getPublic()), salt, info),
                KeyAgreementBox.deriveKey(KeyAgreementBox.sharedSecret(ana.getPrivate(), bank.getPublic()), salt, info));
    }

    @Test
    void anEavesdropperWithAKeyOfHerOwnGetsADifferentKey() {
        KeyPair bank = KeyAgreementBox.newKeyPair();
        KeyPair ana = KeyAgreementBox.newKeyPair();
        KeyPair eve = KeyAgreementBox.newKeyPair();
        assertFalse(Arrays.equals(KeyAgreementBox.sharedSecret(bank.getPrivate(), ana.getPublic()),
                KeyAgreementBox.sharedSecret(eve.getPrivate(), ana.getPublic())));
    }

    @Test
    void aNewSessionKeyPairGivesADifferentKey() {
        KeyPair ana = KeyAgreementBox.newKeyPair();
        byte[] first = KeyAgreementBox.sharedSecret(KeyAgreementBox.newKeyPair().getPrivate(), ana.getPublic());
        byte[] second = KeyAgreementBox.sharedSecret(KeyAgreementBox.newKeyPair().getPrivate(), ana.getPublic());
        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void derivedKeysAreThirtyTwoBytesAndDependOnThePurpose() {
        byte[] secret = new byte[32];
        byte[] a = KeyAgreementBox.deriveKey(secret, Hex.bytes("s"), Hex.bytes("statements"));
        byte[] b = KeyAgreementBox.deriveKey(secret, Hex.bytes("s"), Hex.bytes("messages"));
        assertEquals(32, a.length);
        assertFalse(Arrays.equals(a, b));
    }

    @Test
    void anX25519PublicKeyIs44BytesWhenEncoded() {
        assertEquals(44, KeyAgreementBox.newKeyPair().getPublic().getEncoded().length);
    }

    @Test
    void aWireShowsEveEverythingThatIsSent() {
        Wire wire = new Wire();
        wire.send(new byte[] {1, 2, 3});
        wire.send(new byte[] {4});
        assertEquals(2, wire.seenByEve().size());
        assertArrayEquals(new byte[] {1, 2, 3}, wire.seenByEve().get(0));
    }
}
