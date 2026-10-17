package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;
import org.junit.jupiter.api.Test;

/** X25519 and HKDF against the published test values of RFC 7748 and RFC 5869. */
class StandardVectorsTest {

    private static PrivateKey privateKey(String hex) throws Exception {
        return KeyFactory.getInstance("X25519").generatePrivate(new XECPrivateKeySpec(NamedParameterSpec.X25519, Hex.parse(hex)));
    }

    private static PublicKey publicKey(String hex) throws Exception {
        byte[] littleEndian = Hex.parse(hex);
        byte[] bigEndian = new byte[littleEndian.length];
        for (int i = 0; i < littleEndian.length; i++) {
            bigEndian[i] = littleEndian[littleEndian.length - 1 - i];
        }
        return KeyFactory.getInstance("X25519").generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, new BigInteger(1, bigEndian)));
    }

    private static final String ALICE_PRIVATE = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
    private static final String ALICE_PUBLIC = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    private static final String BOB_PRIVATE = "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb";
    private static final String BOB_PUBLIC = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f";
    private static final String SHARED = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742";

    @Test
    void aliceGetsTheSharedSecretOfRfc7748() throws Exception {
        assertEquals(SHARED, Hex.of(KeyAgreementBox.sharedSecret(privateKey(ALICE_PRIVATE), publicKey(BOB_PUBLIC))));
    }

    @Test
    void bobGetsTheSameSharedSecret() throws Exception {
        assertEquals(SHARED, Hex.of(KeyAgreementBox.sharedSecret(privateKey(BOB_PRIVATE), publicKey(ALICE_PUBLIC))));
    }

    @Test
    void hkdfMatchesTestCaseOneOfRfc5869() {
        byte[] ikm = Hex.parse("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] salt = Hex.parse("000102030405060708090a0b0c");
        byte[] info = Hex.parse("f0f1f2f3f4f5f6f7f8f9");
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
                Hex.of(KeyAgreementBox.hkdf(ikm, salt, info, 42)));
    }
}
