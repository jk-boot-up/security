package com.jk.explore.security.tlsopenssl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** These tests run the openssl command-line tool as the other end of a real TLS connection: run them with ./gradlew integrationTest. They skip themselves if openssl 3 is not installed. */
@Tag("openssl")
class OpenSslInteropTest {

    private static String openssl;

    @TempDir
    Path dir;

    @BeforeAll
    static void find() {
        openssl = OpenSsl.find();
        Assumptions.assumeTrue(openssl != null, "openssl 3 is not installed");
    }

    private OpenSsl.Result connect(int port, String hostName, Path trusted, String... extra) throws Exception {
        String[] fixed = {"s_client", "-connect", "127.0.0.1:" + port, "-servername", TestCertificates.HOST, "-CAfile", trusted.toString(),
            "-verify_hostname", hostName, "-verify_return_error", "-brief", "-ign_eof"};
        String[] all = new String[fixed.length + extra.length];
        System.arraycopy(fixed, 0, all, 0, fixed.length);
        System.arraycopy(extra, 0, all, fixed.length, extra.length);
        return OpenSsl.run(openssl, "order=A-1001\n", all);
    }

    @Test
    void javaClientTalksToOpensslServer() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (OpenSsl.Server server = new OpenSsl.Server(openssl, dir.resolve("shop-cert.pem"), dir.resolve("shop-key.pem"))) {
            TlsClient.Result result = TlsClient.send(TestCertificates.client(), server.port, TestCertificates.HOST, null, "GET / HTTP/1.0\r\n");
            assertTrue(result.ok(), "handshake with s_server");
            assertEquals("TLSv1.3", result.protocol());
            assertEquals("TLS_AES_256_GCM_SHA384", result.cipherSuite());
            assertTrue(result.reply().startsWith("HTTP/1.0 200"), result.reply());
        }
    }

    @Test
    void opensslSeesTheSameProtocolAndCipherAsJava() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            OpenSsl.Result result = connect(server.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"));
            assertEquals(0, result.exit(), result.output());
            assertTrue(result.output().contains("Protocol version: TLSv1.3"), result.output());
            assertTrue(result.output().contains("Ciphersuite: TLS_AES_256_GCM_SHA384"), result.output());
            assertTrue(result.output().contains("Verification: OK"), result.output());
            assertTrue(result.output().contains("received: order=A-1001"), result.output());
        }
    }

    @Test
    void opensslRefusesAWrongHostName() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            OpenSsl.Result result = connect(server.port(), "another.example.test", dir.resolve("shop-cert.pem"));
            assertNotEquals(0, result.exit(), result.output());
            assertTrue(result.output().toLowerCase(Locale.ROOT).contains("hostname mismatch"), result.output());
        }
    }

    @Test
    void opensslRefusesACertificateItDoesNotTrust() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer impostor = new TlsServer(TestCertificates.server("impostor.p12"), null)) {
            OpenSsl.Result result = connect(impostor.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"));
            assertNotEquals(0, result.exit(), result.output());
            assertFalse(result.output().contains("Verification: OK"), result.output());
            assertTrue(result.output().toLowerCase(Locale.ROOT).contains("verif"), result.output());
        }
    }

    @Test
    void opensslNegotiatesTls12WhenAskedAndTheServerAllowsIt() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            OpenSsl.Result result = connect(server.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"), "-tls1_2");
            assertEquals(0, result.exit(), result.output());
            assertTrue(result.output().contains("Protocol version: TLSv1.2"), result.output());
            assertTrue(result.output().contains("received: order=A-1001"), result.output());
        }
    }

    @Test
    void aServerThatRequiresTls13RefusesATls12Client() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), new String[] {"TLSv1.3"})) {
            assertNotEquals(0, connect(server.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"), "-tls1_2").exit());
            assertEquals(0, connect(server.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"), "-tls1_3").exit());
        }
    }

    @Test
    void tls11IsRefused() throws Exception {
        OpenSsl.writeCertAndKey(dir, "shop.p12", "shop");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            OpenSsl.Result result = connect(server.port(), TestCertificates.HOST, dir.resolve("shop-cert.pem"), "-tls1_1");
            assertNotEquals(0, result.exit(), result.output());
            assertFalse(result.output().contains("Protocol version: TLSv1.1"), result.output());
        }
    }
}
