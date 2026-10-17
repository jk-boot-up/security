package com.jk.explore.security.signaturesbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests run a real OpenSSL 3 as an independent check, so they need it installed: run them with ./gradlew integrationTest. */
@Tag("openssl")
class OpenSslInteropTest {

    private Path dir;

    @BeforeEach
    void setUp() throws Exception {
        assertNotNull(OpenSsl.find(), "an OpenSSL 3 must be installed");
        dir = Files.createTempDirectory("signatures-bc");
    }

    @AfterEach
    void tearDown() throws Exception {
        OpenSsl.deleteAll(dir);
    }

    private OpenSsl.Result openssl(String... args) throws Exception {
        return OpenSsl.openssl(dir, args);
    }

    @Test
    void opensslVerifiesADetachedCmsSignature() throws Exception {
        KeyPair keys = Certificates.newEcKeyPair();
        X509Certificate cert = Certificates.selfSigned("Statements", keys);
        byte[] statement = SignaturesBcDemo.STATEMENT;
        Files.write(dir.resolve("statement.txt"), statement);
        Files.write(dir.resolve("signed.der"), Cms.sign(cert, keys.getPrivate(), statement, false, Instant.parse("2026-03-01T09:30:00Z")));
        Files.writeString(dir.resolve("cert.pem"), PemSignatures.write("CERTIFICATE", cert.getEncoded()));
        OpenSsl.Result ok = openssl("cms", "-verify", "-inform", "DER", "-in", dir.resolve("signed.der").toString(),
                "-content", dir.resolve("statement.txt").toString(), "-CAfile", dir.resolve("cert.pem").toString(), "-purpose", "any", "-out", "/dev/null");
        assertEquals(0, ok.exit(), ok.output());
        Files.write(dir.resolve("statement.txt"), Hex.bytes("Statement ANA-1: balance 9200"));
        OpenSsl.Result bad = openssl("cms", "-verify", "-inform", "DER", "-in", dir.resolve("signed.der").toString(),
                "-content", dir.resolve("statement.txt").toString(), "-CAfile", dir.resolve("cert.pem").toString(), "-purpose", "any", "-out", "/dev/null");
        assertNotEquals(0, bad.exit());
    }

    @Test
    void opensslVerifiesAnAttachedCmsSignature() throws Exception {
        KeyPair keys = Certificates.newEcKeyPair();
        X509Certificate cert = Certificates.selfSigned("Statements", keys);
        Files.write(dir.resolve("signed.der"), Cms.sign(cert, keys.getPrivate(), SignaturesBcDemo.STATEMENT, true, Instant.parse("2026-03-01T09:30:00Z")));
        Files.writeString(dir.resolve("cert.pem"), PemSignatures.write("CERTIFICATE", cert.getEncoded()));
        OpenSsl.Result ok = openssl("cms", "-verify", "-inform", "DER", "-in", dir.resolve("signed.der").toString(),
                "-CAfile", dir.resolve("cert.pem").toString(), "-purpose", "any");
        assertEquals(0, ok.exit(), ok.output());
        assertTrue(ok.output().contains("Statement ANA-1: balance 1200"));
    }

    @Test
    void opensslVerifiesAnEd25519Signature() throws Exception {
        byte[] seed = EdDsa.newSeed25519();
        byte[] message = SignaturesBcDemo.STATEMENT;
        Files.write(dir.resolve("m.txt"), message);
        Files.write(dir.resolve("sig.bin"), EdDsa.sign25519(seed, message));
        Files.writeString(dir.resolve("pub.pem"), PemSignatures.publicKey(new Ed25519PrivateKeyParameters(seed, 0).generatePublicKey()));
        OpenSsl.Result ok = openssl("pkeyutl", "-verify", "-pubin", "-inkey", dir.resolve("pub.pem").toString(), "-rawin",
                "-in", dir.resolve("m.txt").toString(), "-sigfile", dir.resolve("sig.bin").toString());
        assertEquals(0, ok.exit(), ok.output());
        Files.write(dir.resolve("m.txt"), Hex.bytes("Statement ANA-1: balance 9200"));
        OpenSsl.Result bad = openssl("pkeyutl", "-verify", "-pubin", "-inkey", dir.resolve("pub.pem").toString(), "-rawin",
                "-in", dir.resolve("m.txt").toString(), "-sigfile", dir.resolve("sig.bin").toString());
        assertNotEquals(0, bad.exit());
    }

    @Test
    void opensslVerifiesBothKindsOfEcdsaSignature() throws Exception {
        KeyPair keys = Certificates.newEcKeyPair();
        BigInteger d = Ecdsa.privateValue(keys.getPrivate());
        byte[] message = SignaturesBcDemo.STATEMENT;
        Files.write(dir.resolve("m.txt"), message);
        Files.writeString(dir.resolve("pub.pem"), PemSignatures.write("PUBLIC KEY", keys.getPublic().getEncoded()));
        for (Ecdsa.Signature signature : new Ecdsa.Signature[] {Ecdsa.signDeterministic(d, message), Ecdsa.signRandomized(d, message)}) {
            Files.write(dir.resolve("sig.der"), signature.der());
            OpenSsl.Result ok = openssl("dgst", "-sha256", "-verify", dir.resolve("pub.pem").toString(),
                    "-signature", dir.resolve("sig.der").toString(), dir.resolve("m.txt").toString());
            assertEquals(0, ok.exit(), ok.output());
        }
    }

    @Test
    void javaVerifiesAnEcdsaSignatureOpensslMade() throws Exception {
        KeyPair keys = Certificates.newEcKeyPair();
        byte[] message = SignaturesBcDemo.STATEMENT;
        Files.write(dir.resolve("m.txt"), message);
        Files.writeString(dir.resolve("key.pem"), PemSignatures.privateKey(keys));
        OpenSsl.Result made = openssl("dgst", "-sha256", "-sign", dir.resolve("key.pem").toString(),
                "-out", dir.resolve("sig.der").toString(), dir.resolve("m.txt").toString());
        assertEquals(0, made.exit(), made.output());
        var pub = Ecdsa.publicFor(Ecdsa.privateValue(keys.getPrivate()));
        assertTrue(Ecdsa.verifyDer(pub, message, Files.readAllBytes(dir.resolve("sig.der"))));
        assertTrue(!Ecdsa.verifyDer(pub, Hex.bytes("other"), Files.readAllBytes(dir.resolve("sig.der"))));
    }

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), SignaturesBcDemo.run());
    }
}
