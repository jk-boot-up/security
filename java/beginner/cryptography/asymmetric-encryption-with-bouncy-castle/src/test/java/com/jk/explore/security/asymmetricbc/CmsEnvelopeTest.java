package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CmsEnvelopeTest {

    private static KeyPair ana;
    private static X509Certificate cert;

    @BeforeAll
    static void make() {
        ana = BouncyCastleDemo.newKeyPair();
        cert = Certificates.selfSigned("Ana", ana);
    }

    @Test
    void anEnvelopeOpensWithTheRecipientsPrivateKey() {
        byte[] statement = BouncyCastleDemo.statement();
        assertArrayEquals(statement, CmsEnvelope.open(ana.getPrivate(), cert, CmsEnvelope.seal(cert, statement)));
    }

    @Test
    void anotherKeyDoesNotOpenIt() {
        KeyPair eve = BouncyCastleDemo.newKeyPair();
        byte[] envelope = CmsEnvelope.seal(cert, Hex.bytes("secret"));
        assertThrows(CmsEnvelope.NotForThisKeyException.class, () -> CmsEnvelope.open(eve.getPrivate(), cert, envelope));
    }

    @Test
    void anEnvelopeAddressedToAnotherCertificateDoesNotOpen() {
        KeyPair other = BouncyCastleDemo.newKeyPair();
        X509Certificate otherCert = Certificates.selfSigned("Other", other);
        byte[] envelope = CmsEnvelope.seal(otherCert, Hex.bytes("secret"));
        assertThrows(CmsEnvelope.NotForThisKeyException.class, () -> CmsEnvelope.open(ana.getPrivate(), cert, envelope));
    }

    @Test
    void theSameDataSealedTwiceGivesDifferentEnvelopes() {
        byte[] a = CmsEnvelope.seal(cert, Hex.bytes("same"));
        byte[] b = CmsEnvelope.seal(cert, Hex.bytes("same"));
        assertFalse(Arrays.equals(a, b));
    }

    @Test
    void theDefaultKeyTransportIsOaepAndTheEasyCallIsPkcs1V15() {
        assertEquals("1.2.840.113549.1.1.7", CmsEnvelope.keyTransportAlgorithm(CmsEnvelope.seal(cert, Hex.bytes("x"), true)));
        assertEquals("1.2.840.113549.1.1.1", CmsEnvelope.keyTransportAlgorithm(CmsEnvelope.seal(cert, Hex.bytes("x"), false)));
    }

    @Test
    void aChangedEnvelopeDoesNotOpenAsTheOriginalStatement() {
        byte[] statement = BouncyCastleDemo.statement();
        byte[] envelope = CmsEnvelope.seal(cert, statement);
        envelope[envelope.length - 20] ^= 1;
        boolean sameText;
        try {
            sameText = Arrays.equals(CmsEnvelope.open(ana.getPrivate(), cert, envelope), statement);
        } catch (RuntimeException e) {
            sameText = false;
        }
        assertFalse(sameText, "CBC without a tag may open to garbage, but never to the original: a reason to prefer authenticated envelopes");
    }
}
