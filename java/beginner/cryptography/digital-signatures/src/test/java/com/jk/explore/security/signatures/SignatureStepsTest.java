package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The by-hand steps give exactly the signature that Java's Ed25519 gives, and the by-hand check agrees with Java's. */
class SignatureStepsTest {

    private final byte[] message = Signing.withContext(Publisher.CONTEXT, SignaturesDemo.list(2, 100, 1200, 25).encode());

    @Test
    void rfc8032TestOneByHand() {
        SignatureSteps.Steps steps = SignatureSteps.sign(SignaturesDemo.SHOP_SEED, new byte[0]);
        assertArrayEquals(SignaturesDemo.SHOP_PUBLIC, steps.publicA());
        assertArrayEquals(Hex.parse("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"),
                steps.signature());
    }

    @Test
    void theStepsGiveJavasSignature() {
        byte[] java = Signing.signRaw(SignaturesDemo.shopPrivate(), message);
        assertArrayEquals(java, SignatureSteps.sign(SignaturesDemo.SHOP_SEED, message).signature());
    }

    @Test
    void theStepsWorkForAnotherKey() {
        byte[] seed = Hex.parse("4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb");   // RFC 8032 test 2
        byte[] java = Signing.signRaw(Ed25519Keys.privateFromSeed(seed), message);
        assertArrayEquals(java, SignatureSteps.sign(seed, message).signature());
    }

    @Test
    void theCheckAcceptsTheRealSignatureOnly() {
        byte[] signature = Signing.signRaw(SignaturesDemo.shopPrivate(), message);
        assertTrue(SignatureSteps.checks(SignaturesDemo.SHOP_PUBLIC, message, signature));
        byte[] changed = message.clone();
        changed[changed.length - 2] ^= 1;
        assertFalse(SignatureSteps.checks(SignaturesDemo.SHOP_PUBLIC, changed, signature));
    }
}
