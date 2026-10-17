package com.jk.explore.security.tlsopenssl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SocketsTest {

    @TempDir
    Path dir;

    @Test
    void clientAndServerAgreeTls13AndTheServerReplies() throws Exception {
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            TlsClient.Result result = TlsClient.send(TestCertificates.client(), server.port(), TestCertificates.HOST, null, "hello");
            assertTrue(result.ok());
            assertEquals("TLSv1.3", result.protocol());
            assertEquals("received: hello", result.reply());
        }
    }

    @Test
    void anUntrustedCertificateIsRefused() throws Exception {
        try (TlsServer impostor = new TlsServer(TestCertificates.server("impostor.p12"), null)) {
            assertFalse(TlsClient.send(TestCertificates.client(), impostor.port(), TestCertificates.HOST, null, "x").ok());
        }
    }

    @Test
    void aWrongNameIsRefused() throws Exception {
        try (TlsServer shop = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            assertFalse(TlsClient.send(TestCertificates.client(), shop.port(), "another.example.test", null, "x").ok());
            assertTrue(TlsClient.send(TestCertificates.client(), shop.port(), TestCertificates.HOST, null, "x").ok());
        }
    }

    @Test
    void versionsAreNegotiated() throws Exception {
        try (TlsServer open = new TlsServer(TestCertificates.server("shop.p12"), null);
                TlsServer modern = new TlsServer(TestCertificates.server("shop.p12"), new String[] {"TLSv1.3"})) {
            assertEquals("TLSv1.2", TlsClient.send(TestCertificates.client(), open.port(), TestCertificates.HOST, new String[] {"TLSv1.2"}, "x").protocol());
            assertFalse(TlsClient.send(TestCertificates.client(), modern.port(), TestCertificates.HOST, new String[] {"TLSv1.2"}, "x").ok());
            assertFalse(TlsClient.send(TestCertificates.client(), open.port(), TestCertificates.HOST, new String[] {"TLSv1.1"}, "x").ok());
        }
    }

    @Test
    void pemRoundTripsThroughTheJdkCertificateParser() throws Exception {
        X509Certificate original = TestCertificates.certificate("shop.p12");
        Path file = dir.resolve("c.pem");
        Pem.writeCertificate(file, original);
        String text = Files.readString(file);
        assertTrue(text.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue(Files.readAllLines(file).stream().allMatch(l -> l.length() <= 64 || l.startsWith("-----")));
        X509Certificate back = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)));
        assertEquals(original, back);
    }

    @Test
    void privateKeyPemIsPkcs8AndOwnerOnly() throws Exception {
        Path file = dir.resolve("k.pem");
        Pem.writePrivateKey(file, TestCertificates.privateKey("shop.p12"));
        String text = Files.readString(file);
        assertTrue(text.startsWith("-----BEGIN PRIVATE KEY-----"));
        String base64 = text.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        assertEquals(TestCertificates.privateKey("shop.p12").getEncoded().length, Base64.getDecoder().decode(base64).length);
        assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    }

    @Test
    void serverCountsFailedHandshakes() throws Exception {
        try (TlsServer impostor = new TlsServer(TestCertificates.server("impostor.p12"), null)) {
            TlsClient.send(TestCertificates.client(), impostor.port(), TestCertificates.HOST, null, "x");
            long deadline = System.currentTimeMillis() + 5000;
            while (impostor.failedHandshakes() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(impostor.failedHandshakes() >= 1);
            assertEquals(0, impostor.served());
        }
    }
}
