package com.jk.explore.security.stepca;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** What a client does with a certificate: build the path to the root it trusts, and check revocation if it was written to. */
final class Pki {

    private Pki() {
    }

    static List<X509Certificate> certificates(Path pem) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(pem)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(c -> (X509Certificate) c).toList();
        }
    }

    static X509Certificate first(Path pem) throws IOException, GeneralSecurityException {
        return certificates(pem).get(0);
    }

    static X509CRL crl(byte[] der) throws GeneralSecurityException {
        return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(der));
    }

    static SSLContext contextTrusting(Path root) throws IOException {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            store.setCertificateEntry("root", first(root));
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    /**
     * "accepted", or "refused (REASON)". The chain in {@code chainPem} is the leaf and its intermediate; {@code root} is the only trust anchor.
     * When {@code checkRevocation} is true the client wants an answer from the list: with no list, or a list that is out of date or not signed
     * by the issuing authority, it refuses. (The list step-ca publishes names its own address in a critical extension that the JDK's built-in
     * checker matches against a distribution point in the certificate, so the check is written out here.)
     */
    static String validate(Path chainPem, Path root, X509CRL crl, boolean checkRevocation, Date when) throws IOException, GeneralSecurityException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> chain = certificates(chainPem);
        PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(first(root), null)));
        params.setDate(when);
        params.setRevocationEnabled(false);
        try {
            CertPathValidator.getInstance("PKIX").validate(factory.generateCertPath(chain), params);
        } catch (CertPathValidatorException e) {
            return "refused (" + (e.getReason() == null ? "INVALID" : e.getReason().toString()) + ")";
        }
        if (!checkRevocation) {
            return "accepted";
        }
        if (crl == null || crl.getNextUpdate() == null || crl.getNextUpdate().before(when)) {
            return "refused (UNDETERMINED_REVOCATION_STATUS)";
        }
        try {
            crl.verify(chain.get(1).getPublicKey());
        } catch (GeneralSecurityException e) {
            return "refused (UNDETERMINED_REVOCATION_STATUS)";
        }
        return crl.isRevoked(chain.get(0)) ? "refused (REVOKED)" : "accepted";
    }
}
