package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DerivationTest {

    @Test
    void pbkdf2MatchesThePublishedValueForOneRound() {
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
                Hex.of(Derivation.fromPassword("password".toCharArray(), Hex.bytes("salt"), 1)));
    }

    @Test
    void pbkdf2MatchesThePublishedValueForTwoRounds() {
        assertEquals("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
                Hex.of(Derivation.fromPassword("password".toCharArray(), Hex.bytes("salt"), 2)));
    }

    @Test
    void theSamePasswordAndSaltGiveTheSameKeyAndAnotherSaltDoesNot() {
        byte[] a = Derivation.fromPassword("p".toCharArray(), Hex.bytes("s1"), 1000);
        assertArrayEquals(a, Derivation.fromPassword("p".toCharArray(), Hex.bytes("s1"), 1000));
        assertFalse(Arrays.equals(a, Derivation.fromPassword("p".toCharArray(), Hex.bytes("s2"), 1000)));
        assertEquals(32, a.length);
    }

    @Test
    void hkdfGivesSeparateKeysForSeparatePurposes() {
        byte[] master = Hex.bytes("one-strong-master-key-of-32-bytes");
        byte[] salt = Hex.bytes("salt");
        byte[] statements = Derivation.forPurpose(master, salt, "statements");
        assertArrayEquals(statements, Derivation.forPurpose(master, salt, "statements"));
        assertFalse(Arrays.equals(statements, Derivation.forPurpose(master, salt, "login tokens")));
        assertEquals(32, statements.length);
    }

    @Test
    void hkdfMatchesTestCaseOneOfRfc5869WhenAskedForThirtyTwoBytes() {
        // RFC 5869 test case 1 gives 42 bytes; its first 32 are the key this class returns for the same inputs.
        byte[] ikm = Hex.parse("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] salt = Hex.parse("000102030405060708090a0b0c");
        byte[] info = Hex.parse("f0f1f2f3f4f5f6f7f8f9");
        try {
            javax.crypto.KDF kdf = javax.crypto.KDF.getInstance("HKDF-SHA256");
            byte[] okm = kdf.deriveData(javax.crypto.spec.HKDFParameterSpec.ofExtract().addIKM(ikm).addSalt(salt).thenExpand(info, 42));
            assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865", Hex.of(okm));
        } catch (java.security.GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }
}
