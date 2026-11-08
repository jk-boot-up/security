package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EnvelopeStepsTest {

    private static KeyPair ana;
    private static X509Certificate cert;

    @BeforeAll
    static void make() {
        ana = BouncyCastleDemo.newKeyPair();
        cert = Certificates.selfSigned("Ana", ana);
    }

    @Test
    void theEnvelopeHoldsALockedKeyAnIvAndTheCipherText() {
        EnvelopeSteps.Parts parts = EnvelopeSteps.parts(CmsEnvelope.seal(cert, BouncyCastleDemo.statement()));
        assertEquals(BigInteger.ONE, parts.serial());
        assertEquals(256, parts.lockedKey().length);
        assertEquals(16, parts.iv().length);
        assertEquals(10_016, parts.cipherText().length);
    }

    @Test
    void openingByHandGivesWhatBouncyCastleGives() {
        byte[] statement = BouncyCastleDemo.statement();
        byte[] envelope = CmsEnvelope.seal(cert, statement);
        EnvelopeSteps.Opened opened = EnvelopeSteps.open(ana.getPrivate(), EnvelopeSteps.parts(envelope));
        assertEquals(32, opened.dataKey().length);
        assertEquals(16, opened.padded()[opened.padded().length - 1]);
        assertArrayEquals(CmsEnvelope.open(ana.getPrivate(), cert, envelope), opened.statement());
        assertArrayEquals(statement, opened.statement());
    }

    @Test
    void anotherPrivateKeyCannotUnlockTheAesKey() {
        KeyPair eve = BouncyCastleDemo.newKeyPair();
        EnvelopeSteps.Parts parts = EnvelopeSteps.parts(CmsEnvelope.seal(cert, Hex.bytes("secret")));
        assertThrows(CmsEnvelope.NotForThisKeyException.class, () -> EnvelopeSteps.open(eve.getPrivate(), parts));
    }
}
