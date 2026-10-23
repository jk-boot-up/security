package com.jk.explore.security.certlife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

/** A certificate chain (leaf first) with its private key, read from files. */
record Credential(List<X509Certificate> chain, PrivateKey key) {

    X509Certificate leaf() {
        return chain.getFirst();
    }

    /** The certificates in PEM and the key in PKCS#8 DER. */
    static Credential load(Path certificates, Path pkcs8Key) throws IOException, GeneralSecurityException {
        PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(pkcs8Key)));
        return new Credential(Pki.readCertificates(certificates), key);
    }
}
