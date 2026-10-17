package com.jk.explore.security.certsbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/** These tests run the openssl command-line tool, so they need openssl 3 on the PATH: run them with ./gradlew integrationTest. */
@Tag("openssl")
class OpenSslInteropTest {

    private static final Instant YESTERDAY = Pki.TODAY.minusSeconds(86400);
    private static Pki pki;

    @TempDir
    Path dir;

    @BeforeAll
    static void make() {
        assertTrue(OpenSsl.available(), "openssl 3 must be on the PATH");
        pki = new Pki();
    }

    private Path write(String name, String text) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, text);
        return file;
    }

    private String attime() {
        return Long.toString(Pki.TODAY.getEpochSecond());
    }

    private void writeChain(Issued server) throws IOException {
        write("root.pem", Pem.of(pki.root.certificate()));
        write("middle.pem", Pem.of(pki.intermediate.certificate()));
        write("server.pem", Pem.of(server.certificate()));
    }

    private OpenSsl.Result verify(String... extra) throws Exception {
        List<String> arguments = new java.util.ArrayList<>(List.of("verify", "-attime", attime()));
        arguments.addAll(List.of(extra));
        arguments.addAll(List.of("-CAfile", "root.pem", "-untrusted", "middle.pem", "server.pem"));
        return OpenSsl.run(dir, arguments.toArray(String[]::new));
    }

    @Test
    void opensslAcceptsTheChain() throws Exception {
        writeChain(pki.server);
        OpenSsl.Result result = verify();
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("server.pem: OK"), result.output());
    }

    @Test
    void opensslRefusesAChainToAnotherRoot() throws Exception {
        writeChain(pki.server);
        write("root.pem", Pem.of(Pki.rogue()[0].certificate()));
        OpenSsl.Result result = verify();
        assertNotEquals(0, result.exit());
    }

    @Test
    void opensslSeesTheExtensionsBouncyCastleWrote() throws Exception {
        writeChain(pki.server);
        String server = OpenSsl.run(dir, "x509", "-in", "server.pem", "-noout", "-text").output();
        assertTrue(server.contains("DNS:shop.example"), server);
        assertTrue(server.contains("CA:FALSE"), server);
        assertTrue(server.contains("Digital Signature"), server);
        String middle = OpenSsl.run(dir, "x509", "-in", "middle.pem", "-noout", "-text").output();
        assertTrue(middle.contains("CA:TRUE, pathlen:0"), middle);
        assertTrue(middle.contains("Certificate Sign, CRL Sign"), middle);
        assertTrue(middle.contains("Name Constraints"), middle);
        assertTrue(middle.contains("Permitted") && middle.contains("DNS:shop.example"), middle);
        assertTrue(middle.contains("ecdsa-with-SHA256"), middle);
    }

    @Test
    void opensslEnforcesTheNameConstraint() throws Exception {
        writeChain(pki.serverFor("other.example", 6));
        OpenSsl.Result result = verify();
        assertNotEquals(0, result.exit());
        assertTrue(result.output().contains("permitted subtree violation"), result.output());
    }

    @Test
    void opensslReadsTheRevocationLists() throws Exception {
        writeChain(pki.server);
        X509CRL root = Crls.empty(pki.root, YESTERDAY);
        write("lists-ok.pem", Pem.of(root) + Pem.of(Crls.empty(pki.intermediate, YESTERDAY)));
        OpenSsl.Result fine = verify("-crl_check_all", "-CRLfile", "lists-ok.pem");
        assertEquals(0, fine.exit(), fine.output());
        X509CRL revoked = Crls.issue(pki.intermediate, YESTERDAY, Map.of(pki.server.certificate().getSerialNumber(), Crls.KEY_COMPROMISE));
        write("lists-revoked.pem", Pem.of(root) + Pem.of(revoked));
        OpenSsl.Result bad = verify("-crl_check_all", "-CRLfile", "lists-revoked.pem");
        assertNotEquals(0, bad.exit());
        assertTrue(bad.output().contains("certificate revoked"), bad.output());
        write("revoked.pem", Pem.of(revoked));
        String text = OpenSsl.run(dir, "crl", "-in", "revoked.pem", "-noout", "-text").output();
        assertTrue(text.contains("Key Compromise"), text);
    }

    @Test
    void opensslVerifiesTheSigningRequest() throws Exception {
        PKCS10CertificationRequest request = SigningRequest.create(CertFactory.newKeys(), "shop.example");
        write("request.pem", Pem.of(request));
        OpenSsl.Result result = OpenSsl.run(dir, "req", "-in", "request.pem", "-noout", "-verify", "-text");
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("verify OK"), result.output());
        assertTrue(result.output().contains("DNS:shop.example"), result.output());
    }

    @Test
    void anOpensslAuthoritySignsABouncyCastleRequestAndTheJdkAcceptsIt() throws Exception {
        KeyPair keys = CertFactory.newKeys();
        write("request.pem", Pem.of(SigningRequest.create(keys, "shop.example")));
        write("ext.cnf", "subjectAltName=DNS:shop.example\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature\n");
        OpenSsl.Result ca = OpenSsl.run(dir, "req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:P-256", "-nodes",
                "-keyout", "ca.key", "-out", "ca.pem", "-subj", "/CN=OpenSSL Test CA", "-days", "30");
        assertEquals(0, ca.exit(), ca.output());
        OpenSsl.Result signed = OpenSsl.run(dir, "x509", "-req", "-in", "request.pem", "-CA", "ca.pem", "-CAkey", "ca.key",
                "-CAcreateserial", "-out", "leaf.pem", "-days", "10", "-extfile", "ext.cnf");
        assertEquals(0, signed.exit(), signed.output());
        X509Certificate leaf = Pem.certificate(Files.readString(dir.resolve("leaf.pem")));
        X509Certificate authority = Pem.certificate(Files.readString(dir.resolve("ca.pem")));
        assertEquals(keys.getPublic(), leaf.getPublicKey());
        ChainValidator validator = new ChainValidator(Set.of(authority));
        assertTrue(validator.validate(List.of(leaf), Instant.now()).accepted());
        assertTrue(HostName.matches(leaf, "shop.example"));
    }
}
