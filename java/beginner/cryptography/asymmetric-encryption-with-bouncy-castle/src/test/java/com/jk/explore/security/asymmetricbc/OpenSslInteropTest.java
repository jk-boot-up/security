package com.jk.explore.security.asymmetricbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests run the openssl command-line tool, so they need openssl 3 on the PATH: run them with ./gradlew integrationTest. */
@Tag("openssl")
class OpenSslInteropTest {

    private static KeyPair ana;
    private static X509Certificate cert;

    @BeforeAll
    static void make() {
        assertTrue(OpenSsl.available(), "openssl 3 must be on the PATH");
        ana = BouncyCastleDemo.newKeyPair();
        cert = Certificates.selfSigned("Ana", ana);
    }

    @Test
    void opensslOpensAnEnvelopeThatBouncyCastleMade() throws Exception {
        byte[] statement = BouncyCastleDemo.statement();
        byte[] envelope = CmsEnvelope.seal(cert, statement);
        OpenSsl.Result result = OpenSsl.decrypt(envelope, Pem.privateKeyInTheClear(ana.getPrivate()), Pem.certificate(cert));
        assertEquals(0, result.exit());
        assertArrayEquals(statement, result.output());
    }

    @Test
    void opensslAlsoOpensTheOlderPkcs1V15Envelope() throws Exception {
        byte[] data = Hex.bytes("short secret");
        byte[] envelope = CmsEnvelope.seal(cert, data, false);
        OpenSsl.Result result = OpenSsl.decrypt(envelope, Pem.privateKeyInTheClear(ana.getPrivate()), Pem.certificate(cert));
        assertArrayEquals(data, result.output());
    }

    @Test
    void opensslRefusesAnotherPrivateKey() throws Exception {
        KeyPair eve = BouncyCastleDemo.newKeyPair();
        byte[] envelope = CmsEnvelope.seal(cert, Hex.bytes("secret"));
        OpenSsl.Result result = OpenSsl.decrypt(envelope, Pem.privateKeyInTheClear(eve.getPrivate()), Pem.certificate(cert));
        assertNotEquals(0, result.exit());
    }

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), BouncyCastleDemo.run());
    }
}
