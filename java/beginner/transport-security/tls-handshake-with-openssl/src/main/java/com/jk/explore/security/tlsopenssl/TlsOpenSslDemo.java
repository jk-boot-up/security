package com.jk.explore.security.tlsopenssl;

import java.io.IOException;
import java.nio.file.Files;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HexFormat;
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

    private static String fingerprint(X509Certificate certificate) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        return HexFormat.of().formatHex(digest, 0, 8);
    }

    private static void act1RealSockets(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "ONE. A real TLS server and client on this machine.");
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            line(out, "  THE REQUEST (a Java client, over a real socket):");
            line(out, "    connects to  " + TestCertificates.HOST + " at " + InetAddress.getLoopbackAddress().getHostAddress());
            line(out, "    sends        " + ORDER);
            TlsClient.Result result = TlsClient.send(TestCertificates.client(), server.port(), TestCertificates.HOST, null, ORDER);
            line(out, "  THE RESPONSE (from the Java server, after the handshake):");
            line(out, "    handshake works: " + result.ok());
            line(out, "    protocol      " + result.protocol());
            line(out, "    cipher suite  " + result.cipherSuite());
            line(out, "    reply         " + result.reply());
        }
        Connection engines = new Connection(TestCertificates.client(), TestCertificates.server("shop.p12"), TestCertificates.HOST);
        line(out, "  two engines in memory agree the same suite: "
                + (engines.handshake() && engines.cipherSuite().equals("TLS_AES_256_GCM_SHA384")));
        line(out, "  anything that speaks TLS can take a side: the tests use openssl");
    }

    private static void act2FilesForOtherPrograms(StringBuilder out, Path dir) throws IOException, GeneralSecurityException {
        line(out, "TWO. Giving the certificate and key to other programs.");
        X509Certificate certificate = TestCertificates.certificate("shop.p12");
        byte[] der = certificate.getEncoded();
        line(out, "  BEFORE PEM (the certificate as Java holds it, DER bytes):");
        line(out, "    " + der.length + " bytes, starting  " + HexFormat.ofDelimiter(" ").formatHex(der, 0, 3) + " ...");
        byte[] three = Arrays.copyOf(der, 3);
        int[] groups = Base64Steps.groups(three);
        StringBuilder bits = new StringBuilder();
        StringBuilder six = new StringBuilder();
        StringBuilder numbers = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            bits.append(i == 0 ? "" : " ").append(Base64Steps.binary(three[i] & 0xff, 8));
        }
        for (int i = 0; i < 4; i++) {
            six.append(i == 0 ? "" : " ").append(Base64Steps.binary(groups[i], 6));
            numbers.append(i == 0 ? "" : " ").append(groups[i]);
        }
        line(out, "  STEP 1. BASE64: 3 BYTES = 24 BITS = 4 GROUPS OF 6 BITS");
        line(out, "    bits    " + bits);
        line(out, "    groups  " + six + "  = " + numbers);
        line(out, "    letters " + Base64Steps.encode(three) + "   (0 = A, 1 = B, 8 = I, 12 = M, 63 = /)");
        Path certFile = dir.resolve("shop-cert.pem");
        Pem.writeCertificate(certFile, certificate);
        List<String> certLines = Files.readAllLines(certFile);
        line(out, "  STEP 2. " + ((der.length + 2) / 3 * 4) + " LETTERS, IN LINES OF 64, BETWEEN TWO LABELS");
        line(out, "  AFTER PEM (shop-cert.pem, " + certLines.size() + " lines):");
        line(out, "    " + certLines.get(0));
        line(out, "    " + certLines.get(1).substring(0, 32) + "...");
        line(out, "    " + certLines.get(certLines.size() - 1));
        Path keyFile = dir.resolve("shop-key.pem");
        Pem.writePrivateKey(keyFile, TestCertificates.privateKey("shop.p12"));
        List<String> keyLines = Files.readAllLines(keyFile);
        line(out, "  THE KEY FILE (shop-key.pem):");
        line(out, "    " + keyLines.get(0) + " ... " + keyLines.get(keyLines.size() - 1));
        line(out, "    for " + certificate.getSubjectX500Principal().getName() + ", key type " + certificate.getPublicKey().getAlgorithm());
        line(out, "    permissions " + PosixFilePermissions.toString(Files.getPosixFilePermissions(keyFile)) + ", owner only");
        line(out, "    the private key, in the clear  <- PROTECT IT LIKE A PASSWORD");
    }

    private static void act3WhoIsOnTheOtherEnd(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "THREE. Who is on the other end.");
        String shopPrint = fingerprint(TestCertificates.certificate("shop.p12"));
        String fakePrint = fingerprint(TestCertificates.certificate("impostor.p12"));
        line(out, "  THE REQUEST (what the client asks for, and trusts):");
        line(out, "    asks for  " + TestCertificates.HOST);
        line(out, "    trusts    one certificate, SHA-256 " + shopPrint);
        try (TlsServer shop = new TlsServer(TestCertificates.server("shop.p12"), null);
                TlsServer impostor = new TlsServer(TestCertificates.server("impostor.p12"), null)) {
            SSLContext client = TestCertificates.client();
            line(out, "  THE RESPONSE (three servers, over real sockets):");
            line(out, "    1. the shop, certificate " + shopPrint);
            line(out, "       works: " + TlsClient.send(client, shop.port(), TestCertificates.HOST, null, ORDER).ok());
            line(out, "    2. an impostor, same name, certificate " + fakePrint);
            line(out, "       works: " + TlsClient.send(client, impostor.port(), TestCertificates.HOST, null, ORDER).ok() + "  <- NOT TRUSTED");
            line(out, "    3. the shop, but the client asked for another.example.test");
            line(out, "       works: " + TlsClient.send(client, shop.port(), "another.example.test", null, ORDER).ok() + "  <- THE NAME MUST MATCH");
        }
        line(out, "  openssl s_client checks the same two things when given");
        line(out, "  -CAfile, -verify_hostname NAME and -verify_return_error");
    }

    private static String offer(SSLContext client, TlsServer server, String version) {
        TlsClient.Result result = TlsClient.send(client, server.port(), TestCertificates.HOST, new String[] {version}, ORDER);
        return "client offers " + (version + "   ").substring(0, 7) + "  works: " + result.ok()
                + (result.ok() ? ", agreed " + result.protocol() : "");
    }

    private static void act4Versions(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "FOUR. Versions: what each side offers.");
        SSLContext client = TestCertificates.client();
        SSLContext shop = TestCertificates.server("shop.p12");
        try (TlsServer open = new TlsServer(shop, null); TlsServer modern = new TlsServer(shop, new String[] {"TLSv1.3"})) {
            line(out, "  BEFORE TIGHTENING (a server with the JDK's defaults):");
            line(out, "    " + offer(client, open, "TLSv1.1") + "  <- OLD VERSIONS ARE OFF");
            line(out, "    " + offer(client, open, "TLSv1.2"));
            line(out, "  AFTER TIGHTENING (the server allows only TLS 1.3):");
            line(out, "    " + offer(client, modern, "TLSv1.2"));
            line(out, "    " + offer(client, modern, "TLSv1.3"));
        }
        line(out, "  openssl s_client -tls1_1, -tls1_2 and -tls1_3 make the same offers");
    }

    private static void act5Bill(StringBuilder out) throws IOException, GeneralSecurityException {
        line(out, "FIVE. Checking from the outside, and the bill.");
        TlsClient.Result result;
        try (TlsServer server = new TlsServer(TestCertificates.server("shop.p12"), null)) {
            result = TlsClient.send(TestCertificates.client(), server.port(), TestCertificates.HOST, null, ORDER);
        }
        line(out, "  THE REQUEST (openssl as the client, the command):");
        line(out, "    openssl s_client -connect HOST:PORT -servername NAME");
        line(out, "      -CAfile cert.pem -verify_hostname NAME");
        line(out, "      -verify_return_error -brief");
        line(out, "  THE RESPONSE (the lines the tests check, for this server):");
        line(out, "    Protocol version: " + result.protocol());
        line(out, "    Ciphersuite: " + result.cipherSuite());
        line(out, "    Verification: OK");
        line(out, "    " + result.reply());
        line(out, "  openssl as the server: s_server -cert cert.pem -key key.pem");
        line(out, "  THE BILL:");
        line(out, "    a second program catches mistakes in the first,");
        line(out, "    but only the settings you pass are checked");
        line(out, "    without -verify_return_error, openssl reports and goes on,");
        line(out, "    so a script must check the exit status");
        line(out, "    a key file for another program is one more copy to protect");
    }
}
