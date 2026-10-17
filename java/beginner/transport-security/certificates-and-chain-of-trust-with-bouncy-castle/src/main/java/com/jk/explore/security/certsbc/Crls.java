package com.jk.explore.security.certsbc;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** A certificate revocation list: the issuer's signed statement of which of its certificates should no longer be trusted. */
final class Crls {

    private Crls() {
    }

    /** The reasons listed in RFC 5280 that this project uses. */
    static final int KEY_COMPROMISE = CRLReason.keyCompromise;
    static final int CA_COMPROMISE = CRLReason.cACompromise;

    /** A list valid for a week from the given day, signed by the issuer, naming the revoked serial numbers and why. */
    static X509CRL issue(Issued issuer, Instant thisUpdate, Map<BigInteger, Integer> revoked) {
        try {
            X509v2CRLBuilder builder = new X509v2CRLBuilder(new JcaX509CertificateHolder(issuer.certificate()).getSubject(), Date.from(thisUpdate));
            builder.setNextUpdate(Date.from(thisUpdate.plusSeconds(7 * 24 * 3600L)));
            for (Map.Entry<BigInteger, Integer> entry : new LinkedHashMap<>(revoked).entrySet()) {
                builder.addCRLEntry(entry.getKey(), Date.from(thisUpdate), entry.getValue());
            }
            builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuer.certificate()));
            return new JcaX509CRLConverter().getCRL(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(issuer.keys().getPrivate())));
        } catch (Exception e) {
            throw new IllegalStateException("could not build the list", e);
        }
    }

    static X509CRL empty(Issued issuer, Instant thisUpdate) {
        return issue(issuer, thisUpdate, Map.of());
    }

    static int entries(X509CRL crl) {
        return crl.getRevokedCertificates() == null ? 0 : crl.getRevokedCertificates().size();
    }

    static boolean lists(X509CRL crl, BigInteger serial) {
        X509CRLEntry entry = crl.getRevokedCertificate(serial);
        return entry != null;
    }

    /** Whether the list's signature verifies with the given issuer's public key. */
    static boolean signedBy(X509CRL crl, Issued issuer) {
        try {
            crl.verify(issuer.certificate().getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
