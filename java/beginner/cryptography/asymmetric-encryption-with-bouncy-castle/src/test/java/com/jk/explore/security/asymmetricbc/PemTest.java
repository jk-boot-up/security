package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PemTest {

    private static KeyPair keys;

    @BeforeAll
    static void makeKeys() {
        keys = BouncyCastleDemo.newKeyPair();
    }

    @Test
    void aPublicKeyIsWrittenWithAHeaderAndReadsBack() {
        String pem = Pem.publicKey(keys.getPublic());
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pem.trim().endsWith("-----END PUBLIC KEY-----"));
        assertEquals(keys.getPublic(), Pem.readPublicKey(pem));
    }

    @Test
    void aPrivateKeyInTheClearIsPkcs8AndReadsBack() {
        String pem = Pem.privateKeyInTheClear(keys.getPrivate());
        assertTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----"));
        assertEquals(keys.getPrivate(), Pem.readPrivateKey(pem));
    }

    @Test
    void anEncryptedPrivateKeyHidesTheKeyAndNeedsThePassword() {
        String clear = Pem.privateKeyInTheClear(keys.getPrivate());
        String locked = Pem.privateKeyEncrypted(keys.getPrivate(), BouncyCastleDemo.PASSWORD);
        assertTrue(locked.startsWith("-----BEGIN ENCRYPTED PRIVATE KEY-----"));
        String clearBody = clear.lines().skip(1).findFirst().orElseThrow();
        assertFalse(locked.contains(clearBody));
        assertEquals(keys.getPrivate(), Pem.readEncryptedPrivateKey(locked, BouncyCastleDemo.PASSWORD));
    }

    @Test
    void aWrongPasswordIsRefused() {
        String locked = Pem.privateKeyEncrypted(keys.getPrivate(), BouncyCastleDemo.PASSWORD);
        assertThrows(Pem.WrongPasswordException.class, () -> Pem.readEncryptedPrivateKey(locked, "wrong".toCharArray()));
    }

    @Test
    void theSamePasswordGivesADifferentFileEachTimeBecauseOfTheSalt() {
        String a = Pem.privateKeyEncrypted(keys.getPrivate(), BouncyCastleDemo.PASSWORD);
        String b = Pem.privateKeyEncrypted(keys.getPrivate(), BouncyCastleDemo.PASSWORD);
        assertFalse(a.equals(b));
    }
}
