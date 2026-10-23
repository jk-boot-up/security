package com.jk.explore.security.httpsspring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.HexFormat;

/**
 * The demo keystores, made with keytool on the first build into build/ and never committed (see infra/make-test-keystores.sh): a root CA's trust store, two server certificates for
 * shop.example.test signed by it (the second stands for a renewed certificate), a client certificate, and an impostor's certificate signed by nobody.
 * The password is a demo value, and the keys protect nothing. They are copied to a folder because Spring's SSL bundles read files, and watch them for changes.
 */
final class Certificates {

    static final String PASSWORD = "demo-password";
    static final String HOST = "shop.example.test";

    private Certificates() {
    }

    /** Copies the named keystores from the class path into the folder. */
    static void install(Path folder, String... names) throws IOException {
        for (String name : names) {
            try (InputStream in = Certificates.class.getResourceAsStream("/" + name)) {
                if (in == null) {
                    throw new IOException("missing resource " + name);
                }
                Files.copy(in, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static String fingerprint(Certificate certificate) throws GeneralSecurityException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }

    /** The fingerprint of the certificate under the alias in a PKCS12 file. */
    static String fingerprintOf(Path keystore, String alias) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystore)) {
            store.load(in, PASSWORD.toCharArray());
        }
        return fingerprint(store.getCertificate(alias));
    }
}
