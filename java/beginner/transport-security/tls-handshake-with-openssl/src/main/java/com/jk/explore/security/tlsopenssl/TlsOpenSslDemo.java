package com.jk.explore.security.tlsopenssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;

/**
 * Runs the online store story in five numbered acts, over real sockets on this machine. It prints only what the Java side can know and
 * what does not change between runs: no ports, no random values. The openssl side is in the integration tests.
 */
public class TlsOpenSslDemo {

    static final String ORDER = "order=A-1001";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, GeneralSecurityException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("tls-openssl-demo");
        try {
            act1RealSockets(out);
            act2FilesForOtherPrograms(out, dir);
            act3WhoIsOnTheOtherEnd(out);
            act4Versions(out);
            act5Bill(out);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1RealSockets(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "ONE. A real TLS server and client on this machine.");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            TlsClient.Result result = TlsClient.send(TestCertificates.client(), server.port(), TestCertificates.HOST, null, ORDER);
            line(out, "  the server listens on the loopback address; the client connects by the name " + TestCertificates.HOST);
            line(out, "  handshake works: " + result.ok() + ", agreed protocol: " + result.protocol());
            line(out, "  agreed cipher suite: " + result.cipherSuite());
            line(out, "  the server answered: " + result.reply());
        }
        Connection engines = new Connection(TestCertificates.client(), TestCertificates.server("shop.p12"), TestCertificates.HOST);
        line(out, "  two engines wired together in memory agree the same suite: " + (engines.handshake() && engines.cipherSuite().equals("TLS_AES_256_GCM_SHA384")));
        line(out, "  anything that speaks TLS can take the client's place, so the tests let the openssl program do it");
    }

    private static void act2FilesForOtherPrograms(StringBuilder out, Path dir) throws IOException, GeneralSecurityException {
        line(out, "TWO. Giving the certificate and key to other programs.");
        X509Certificate certificate = TestCertificates.certificate("shop.p12");
        Path certFile = dir.resolve("shop-cert.pem");
        Path keyFile = dir.resolve("shop-key.pem");
        Pem.writeCertificate(certFile, certificate);
        Pem.writePrivateKey(keyFile, TestCertificates.privateKey("shop.p12"));
        List<String> certLines = Files.readAllLines(certFile);
        List<String> keyLines = Files.readAllLines(keyFile);
        line(out, "  the certificate file starts with " + certLines.get(0) + " and ends with " + certLines.get(certLines.size() - 1));
        line(out, "  the key file starts with " + keyLines.get(0) + " and ends with " + keyLines.get(keyLines.size() - 1));
        line(out, "  the certificate names " + certificate.getSubjectX500Principal().getName() + " and the key type is " + certificate.getPublicKey().getAlgorithm());
        line(out, "  the key file is readable by its owner only: "
                + Files.getPosixFilePermissions(keyFile).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        line(out, "  PEM is only Base64 of the same bytes, so openssl reads what Java wrote and Java reads what openssl writes");
        line(out, "  the key file holds the private key in the clear  <- PROTECT IT LIKE A PASSWORD");
    }

    private static void act3WhoIsOnTheOtherEnd(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "THREE. Who is on the other end.");
        try (TlsServer shop = new TlsServer(TestCertificates.server("shop.p12"), null);
                TlsServer impostor = new TlsServer(TestCertificates.server("impostor.p12"), null)) {
            SSLContext client = TestCertificates.client();
            line(out, "  the shop's own certificate, name " + TestCertificates.HOST + ": handshake works: "
                    + TlsClient.send(client, shop.port(), TestCertificates.HOST, null, ORDER).ok());
            line(out, "  an impostor with the same name but a certificate the client does not trust: handshake works: "
                    + TlsClient.send(client, impostor.port(), TestCertificates.HOST, null, ORDER).ok() + "  <- THE CLIENT REFUSES");
            line(out, "  the shop's real certificate, but the client asked for another.example.test: handshake works: "
                    + TlsClient.send(client, shop.port(), "another.example.test", null, ORDER).ok() + "  <- THE NAME MUST MATCH");
        }
        line(out, "  openssl s_client checks the same two things when given -verify_return_error and -verify_hostname");
    }

    private static void act4Versions(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "FOUR. Versions: what each side offers.");
        SSLContext client = TestCertificates.client();
        SSLContext shop = TestCertificates.server("shop.p12");
        try (TlsServer open = new TlsServer(shop, null); TlsServer modern = new TlsServer(shop, new String[] {"TLSv1.3"})) {
            line(out, "  a client that offers only TLS 1.2 to a server that allows the defaults: handshake works: "
                    + TlsClient.send(client, open.port(), TestCertificates.HOST, new String[] {"TLSv1.2"}, ORDER).ok());
            TlsClient.Result twelve = TlsClient.send(client, open.port(), TestCertificates.HOST, new String[] {"TLSv1.2"}, ORDER);
            line(out, "  the agreed protocol is " + twelve.protocol());
            line(out, "  the same client against a server that allows only TLS 1.3: handshake works: "
                    + TlsClient.send(client, modern.port(), TestCertificates.HOST, new String[] {"TLSv1.2"}, ORDER).ok());
            line(out, "  a client that offers only TLS 1.1: handshake works: "
                    + TlsClient.send(client, open.port(), TestCertificates.HOST, new String[] {"TLSv1.1"}, ORDER).ok() + "  <- OLD VERSIONS ARE SWITCHED OFF");
        }
        line(out, "  openssl s_client -tls1_2 and -tls1_1 make the same offers, and the tests check what it is told");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. Checking from the outside, and the bill.");
        line(out, "  to check a server with another program, run: openssl s_client -connect HOST:PORT -servername NAME -CAfile cert.pem -verify_hostname NAME -verify_return_error");
        line(out, "  to serve with another program, run: openssl s_server -accept PORT -cert cert.pem -key key.pem");
        line(out, "  the bill: a second implementation catches mistakes in the first, but only the settings that you actually pass are checked");
        line(out, "  openssl without -verify_return_error prints the problem and carries on, so a script must check the exit status and the verification line");
        line(out, "  a private key written to a file for another program is one more copy to protect");
    }
}
