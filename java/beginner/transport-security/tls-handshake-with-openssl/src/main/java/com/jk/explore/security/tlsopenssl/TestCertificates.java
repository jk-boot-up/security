package com.jk.explore.security.tlsopenssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Two PKCS12 keystores made once with keytool: the shop's, and an impostor's that claims the same name but is not trusted.
 * The password is a demo value, and the keys protect nothing. The certificates are valid for a hundred years, so the demo never expires.
 */
final class TestCertificates {

    static final String HOST = "shop.example.test";
    private static final char[] PASSWORD = "demo-password".toCharArray();

    private TestCertificates() {
    }

    private static KeyStore load(String resource) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = TestCertificates.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                throw new IOException("missing resource " + resource);
            }
            store.load(in, PASSWORD);
        }
        return store;
    }

    /** A server context that presents the named keystore's certificate. */
    static SSLContext server(String keystore) throws GeneralSecurityException, IOException {
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(load(keystore), PASSWORD);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        return context;
    }

    /** A client context that trusts only the shop's certificate. */
    static SSLContext client() throws GeneralSecurityException, IOException {
        Certificate shop = load("shop.p12").getCertificate("shop");
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("shop", shop);
        TrustManagerFactory trusts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trusts.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trusts.getTrustManagers(), null);
        return context;
    }

    static X509Certificate certificate(String keystore) throws GeneralSecurityException, IOException {
        return (X509Certificate) load(keystore).getCertificate(keystore.startsWith("shop") ? "shop" : "impostor");
    }

    static PrivateKey privateKey(String keystore) throws GeneralSecurityException, IOException {
        return (PrivateKey) load(keystore).getKey(keystore.startsWith("shop") ? "shop" : "impostor", PASSWORD);
    }
}
