package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PkiTest {

    private static X509Certificate certificate(String name) throws Exception {
        try (InputStream in = PkiTest.class.getResourceAsStream("/pki/" + name)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    @Test
    void theShopCertificateIsSignedByTheDemoRootAndNamesTheShop() throws Exception {
        X509Certificate root = certificate("root.pem");
        X509Certificate shop = certificate("shop.crt");
        shop.verify(root.getPublicKey());
        shop.checkValidity();
        Collection<List<?>> names = shop.getSubjectAlternativeNames();
        assertTrue(names.stream().anyMatch(n -> n.get(1).equals("shop.example.test")));
        assertEquals(List.of("1.3.6.1.5.5.7.3.1"), shop.getExtendedKeyUsage());
        assertEquals(-1, shop.getBasicConstraints());
    }

    @Test
    void theImpostorHasTheSameNameButIsNotSignedByTheRoot() throws Exception {
        X509Certificate root = certificate("root.pem");
        X509Certificate impostor = certificate("impostor.crt");
        assertTrue(impostor.getSubjectAlternativeNames().stream().anyMatch(n -> n.get(1).equals("shop.example.test")));
        assertThrows(SignatureException.class, () -> impostor.verify(root.getPublicKey()));
    }

    @Test
    void theRootIsACertificateAuthority() throws Exception {
        X509Certificate root = certificate("root.pem");
        assertTrue(root.getBasicConstraints() >= 0);
        root.verify(root.getPublicKey());
    }

    @Test
    void anasKeystoreHasAKeyAndAClientCertificate() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = PkiTest.class.getResourceAsStream("/pki/client.p12")) {
            store.load(in, Pki.PASSWORD);
        }
        assertTrue(store.isKeyEntry("ana"));
        X509Certificate ana = (X509Certificate) store.getCertificate("ana");
        assertEquals("CN=Ana the customer", ana.getSubjectX500Principal().getName());
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), ana.getExtendedKeyUsage());
        ana.verify(certificate("root.pem").getPublicKey());
    }

    @Test
    void theTrustStoreHoldsOnlyTheDemoRoot() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = PkiTest.class.getResourceAsStream("/pki/trust.p12")) {
            store.load(in, Pki.PASSWORD);
        }
        assertEquals(1, store.size());
        assertNotNull(store.getCertificate("root"));
        assertFalse(store.isKeyEntry("root"));
    }

    @Test
    void theFilesForNginxAreCopiedAndReadable(@TempDir Path dir) throws Exception {
        Path target = Files.createDirectory(dir.resolve("pki"));
        Pki.copyForNginx(target);
        for (String name : List.of("root.pem", "shop.crt", "shop.key", "impostor.crt", "impostor.key")) {
            assertTrue(Files.isReadable(target.resolve(name)), name);
        }
        assertFalse(Files.exists(target.resolve("client.p12")), "the client's key never goes to the server");
    }

    @Test
    void clientContextsCanBeBuilt() throws Exception {
        assertNotNull(Pki.sslContext(true, true));
        assertNotNull(Pki.sslContext(false, false));
    }
}
