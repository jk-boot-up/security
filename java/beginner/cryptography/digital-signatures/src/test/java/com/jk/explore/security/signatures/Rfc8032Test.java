package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Ed25519 against the test vectors of RFC 8032, section 7.1. */
class Rfc8032Test {

    @ParameterizedTest
    @CsvSource(value = {
        "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60, d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a, '', "
            + "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
        "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb, 3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c, 72, "
            + "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"
    })
    void signaturesMatchTheRfcAndVerify(String seed, String publicKey, String message, String signature) {
        var priv = Ed25519Keys.privateFromSeed(Hex.parse(seed));
        var pub = Ed25519Keys.publicFromRaw(Hex.parse(publicKey));
        byte[] msg = message.isEmpty() ? new byte[0] : Hex.parse(message);
        byte[] made = Signing.signRaw(priv, msg);
        assertEquals(signature, Hex.of(made));
        assertTrue(Signing.verifyRaw(pub, msg, Hex.parse(signature)));
    }

    @org.junit.jupiter.api.Test
    void aChangedMessageOrSignatureDoesNotVerify() {
        var pub = Ed25519Keys.publicFromRaw(Hex.parse("3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c"));
        byte[] good = Hex.parse("92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00");
        assertFalse(Signing.verifyRaw(pub, Hex.parse("73"), good));
        byte[] bad = good.clone();
        bad[0] ^= 1;
        assertFalse(Signing.verifyRaw(pub, Hex.parse("72"), bad));
    }

    @org.junit.jupiter.api.Test
    void aWrongLengthSignatureIsRefusedNotAnError() {
        var pub = Ed25519Keys.publicFromRaw(Hex.parse("3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c"));
        assertFalse(Signing.verifyRaw(pub, Hex.parse("72"), new byte[10]));
    }

    @org.junit.jupiter.api.Test
    void aRawPublicKeyIs32BytesAndEncodesTo44() {
        var pub = Ed25519Keys.publicFromRaw(Hex.parse("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"));
        assertEquals(44, pub.getEncoded().length);
    }
}
