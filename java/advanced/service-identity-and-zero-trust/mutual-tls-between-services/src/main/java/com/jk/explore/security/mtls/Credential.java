package com.jk.explore.security.mtls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

/** A certificate chain with its private key, read from files. */
record Credential(List<X509Certificate> chain, PrivateKey key) {

    X509Certificate leaf() {
        return chain.getFirst();
    }

    /** The certificate in PEM and the key in PKCS#8 DER, as step writes them. */
    static Credential load(Path certificate, Path pkcs8Key) throws IOException, GeneralSecurityException {
        PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(pkcs8Key)));
        return new Credential(readCertificates(certificate), key);
    }

    static List<X509Certificate> readCertificates(Path pem) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(pem)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(c -> (X509Certificate) c).toList();
        }
    }
}
