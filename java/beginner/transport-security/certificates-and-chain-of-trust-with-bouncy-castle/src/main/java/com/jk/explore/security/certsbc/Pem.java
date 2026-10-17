package com.jk.explore.security.certsbc;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/** PEM is the text form every tool reads: a line saying what it is, the DER bytes in base 64, and a closing line. */
final class Pem {

    private Pem() {
    }

    static String of(Object object) {
        try (StringWriter text = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(text)) {
            writer.writeObject(object);
            writer.flush();
            return text.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String of(X509Certificate certificate) {
        return of((Object) certificate);
    }

    static String of(X509CRL list) {
        return of((Object) list);
    }

    static String of(PKCS10CertificationRequest request) {
        return of((Object) request);
    }

    static X509Certificate certificate(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            if (object instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter().getCertificate(holder);
            }
            throw new IllegalArgumentException("not a certificate");
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("not a certificate", e);
        }
    }
}
