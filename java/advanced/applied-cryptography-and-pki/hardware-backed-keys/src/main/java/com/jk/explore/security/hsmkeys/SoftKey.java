package com.jk.explore.security.hsmkeys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

/** The contrast: the same signing key as a PKCS12 file protected by a password. */
final class SoftKey {

    private SoftKey() {
    }

    /** Writes a keystore file with a fresh RSA key and a self-signed certificate; returns the certificate. */
    static Certificate create(Path file, String password) throws GeneralSecurityException, IOException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        byte[] der = Der.selfSignedRsa(Provisioning.ORG, Provisioning.CN, kp.getPublic(), BigInteger.valueOf(2002), "260101000000Z", "360101000000Z", tbs -> {
            try {
                Signature s = Signature.getInstance("SHA256withRSA");
                s.initSign(kp.getPrivate());
                s.update(tbs);
                return s.sign();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        });
        Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(Lab.KEY_ALIAS, kp.getPrivate(), password.toCharArray(), new Certificate[] {cert});
        try (OutputStream out = Files.newOutputStream(file)) {
            ks.store(out, password.toCharArray());
        }
        return cert;
    }

    /** Tries the passwords in order against a copy of the file; returns the 1-based number of the guess that opened it, or 0. */
    static int crack(Path file, List<String> guesses) throws IOException {
        for (int i = 0; i < guesses.size(); i++) {
            try (InputStream in = Files.newInputStream(file)) {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(in, guesses.get(i).toCharArray());
                return i + 1;
            } catch (IOException | GeneralSecurityException wrong) {
                // wrong password: try the next one
            }
        }
        return 0;
    }

    static PrivateKey open(Path file, String password) throws GeneralSecurityException, IOException {
        try (InputStream in = Files.newInputStream(file)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, password.toCharArray());
            return (PrivateKey) ks.getKey(Lab.KEY_ALIAS, password.toCharArray());
        }
    }
}
