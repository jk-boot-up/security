package com.jk.explore.security.asymmetricbc;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;

/**
 * Keys and certificates as PEM: the text format with a "-----BEGIN ...-----" line, Base64 in the middle and an "-----END ...-----"
 * line, which OpenSSL, SSH tools, web servers and most libraries read and write. The JDK has no PEM support; Bouncy Castle does.
 */
public final class Pem {

    private Pem() {
    }

    public static String publicKey(PublicKey key) {
        return write(key);
    }

    /** The private key as PKCS#8, in the clear: whoever can read this text has the key. */
    public static String privateKeyInTheClear(PrivateKey key) {
        try {
            return write(new JcaPKCS8Generator(key, null));
        } catch (PemGenerationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The private key as PKCS#8 encrypted with a password (AES-256 with a key made from the password). */
    public static String privateKeyEncrypted(PrivateKey key, char[] password) {
        Providers.ensure();
        try {
            var encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                    .setProvider(Providers.BC).setPassword(password).build();
            return write(new JcaPKCS8Generator(key, encryptor));
        } catch (OperatorCreationException | PemGenerationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String certificate(X509Certificate certificate) {
        return write(certificate);
    }

    public static PublicKey readPublicKey(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            return new JcaPEMKeyConverter().setProvider(Providers.BC).getPublicKey((SubjectPublicKeyInfo) object);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static PrivateKey readPrivateKey(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            return new JcaPEMKeyConverter().setProvider(Providers.BC).getPrivateKey((PrivateKeyInfo) object);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Reads an encrypted private key, or throws {@link WrongPasswordException} if the password is wrong. */
    public static PrivateKey readEncryptedPrivateKey(String pem, char[] password) {
        Providers.ensure();
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            PKCS8EncryptedPrivateKeyInfo encrypted = (PKCS8EncryptedPrivateKeyInfo) parser.readObject();
            var decryptor = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider(Providers.BC).build(password);
            return new JcaPEMKeyConverter().setProvider(Providers.BC).getPrivateKey(encrypted.decryptPrivateKeyInfo(decryptor));
        } catch (Exception e) {
            throw new WrongPasswordException("the private key did not open with that password");
        }
    }

    public static X509Certificate readCertificate(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            X509CertificateHolder holder = (X509CertificateHolder) parser.readObject();
            return new JcaX509CertificateConverter().setProvider(Providers.BC).getCertificate(holder);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String write(Object object) {
        Providers.ensure();
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            writer.writeObject(object);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    /** Thrown when an encrypted private key is opened with the wrong password. */
    public static class WrongPasswordException extends RuntimeException {
        public WrongPasswordException(String message) {
            super(message);
        }
    }
}
