package com.jk.explore.security.httpsspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertificatesTest {

    @TempDir
    Path dir;

    private KeyStore load(String name) throws Exception {
        Certificates.install(dir, name);
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(dir.resolve(name))) {
            store.load(in, Certificates.PASSWORD.toCharArray());
        }
        return store;
    }

    @Test
    void installCopiesFilesAndRejectsUnknownNames() throws Exception {
        Certificates.install(dir, "shop.p12", "trust.p12");
        assertTrue(Files.exists(dir.resolve("shop.p12")));
        assertThrows(IOException.class, () -> Certificates.install(dir, "missing.p12"));
    }

    @Test
    void theTwoServerCertificatesAreDifferentButNameTheSameHost() throws Exception {
        Certificates.install(dir, "shop.p12", "shop2.p12");
        assertNotEquals(Certificates.fingerprintOf(dir.resolve("shop.p12"), "shop"), Certificates.fingerprintOf(dir.resolve("shop2.p12"), "shop"));
        for (String name : List.of("shop.p12", "shop2.p12")) {
            X509Certificate certificate = (X509Certificate) load(name).getCertificate("shop");
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            assertTrue(names.stream().anyMatch(n -> n.get(1).equals(Certificates.HOST)));
        }
    }

    @Test
    void fingerprintIsSha256Hex() throws Exception {
        Certificates.install(dir, "shop.p12");
        assertEquals(64, Certificates.fingerprintOf(dir.resolve("shop.p12"), "shop").length());
    }

    @Test
    void bothServerCertificatesChainToTheDemoRoot() throws Exception {
        KeyStore trust = load("trust.p12");
        PKIXParameters parameters = new PKIXParameters(trust);
        parameters.setRevocationEnabled(false);
        for (String name : List.of("shop.p12", "shop2.p12")) {
            X509Certificate leaf = (X509Certificate) load(name).getCertificate("shop");
            var path = CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf));
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
        }
    }

    @Test
    void theImpostorDoesNotChainToTheDemoRoot() throws Exception {
        PKIXParameters parameters = new PKIXParameters(load("trust.p12"));
        parameters.setRevocationEnabled(false);
        X509Certificate leaf = (X509Certificate) load("impostor.p12").getCertificate("shop");
        var path = CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf));
        assertThrows(java.security.cert.CertPathValidatorException.class, () -> CertPathValidator.getInstance("PKIX").validate(path, parameters));
    }

    @Test
    void clientCertificateIsForClientAuthentication() throws Exception {
        X509Certificate client = (X509Certificate) load("client.p12").getCertificate("ana");
        assertTrue(client.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2"));
        assertEquals("CN=Ana the customer", client.getSubjectX500Principal().getName());
    }
}
