package com.jk.explore.security.ca;

import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a client does with a server's certificate: build the chain to a trusted root, check the dates and the revocation list at a given moment, check the
 * name in the <em>subject alternative name</em> (never the common name), and check that the certificate may be used for a TLS server.
 */
public final class Verifier {

    private final X509Certificate trustedRoot;

    public Verifier(X509Certificate trustedRoot) {
        this.trustedRoot = trustedRoot;
    }

    /** Throws {@link Untrusted} with a reason, or returns normally. {@code chain} starts with the server's own certificate. */
    public void check(List<X509Certificate> chain, String host, Collection<X509CRL> crls, Instant at) {
        try {
            PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(trustedRoot, null)));
            params.setDate(Date.from(at));
            params.setRevocationEnabled(true); // a certificate with no current revocation list is refused: hard fail
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            CertPathValidator.getInstance("PKIX").validate(path, params);
        } catch (CertPathValidatorException e) {
            throw new Untrusted(reason(e));
        } catch (java.security.GeneralSecurityException e) {
            throw new Untrusted("could not build the chain: " + e.getClass().getSimpleName());
        }
        X509Certificate leaf = chain.get(0);
        try {
            List<String> eku = leaf.getExtendedKeyUsage();
            if (eku == null || !eku.contains("1.3.6.1.5.5.7.3.1")) {
                throw new Untrusted("certificate is not for a TLS server");
            }
        } catch (java.security.cert.CertificateParsingException e) {
            throw new Untrusted("unreadable extensions");
        }
        if (!nameMatches(leaf, host)) {
            throw new Untrusted("name " + host + " is not in the certificate");
        }
    }

    private static String reason(CertPathValidatorException e) {
        String m = String.valueOf(e.getMessage());
        if (e.getReason() == CertPathValidatorException.BasicReason.EXPIRED) {
            return "certificate expired";
        }
        if (e.getReason() == CertPathValidatorException.BasicReason.NOT_YET_VALID) {
            return "certificate not yet valid";
        }
        if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
            return "certificate revoked";
        }
        if (m.contains("revocation status") || m.contains("CRL")) {
            return "revocation status unknown (no current list)";
        }
        if (m.contains("basic constraints") || m.contains("CA")) {
            return "a certificate in the chain may not sign certificates";
        }
        if (m.contains("signature") || m.contains("Signature")) {
            return "signature does not match the issuer";
        }
        if (m.contains("anchor") || m.contains("trust")) {
            return "not issued by a trusted authority";
        }
        return "chain not trusted";
    }

    /** Exact match, or a wildcard in the left-most label only ({@code *.shop.example} matches {@code www.shop.example}, not {@code a.b.shop.example}). */
    static boolean nameMatches(X509Certificate leaf, String host) {
        try {
            var names = leaf.getSubjectAlternativeNames();
            if (names == null) {
                return false;
            }
            String h = host.toLowerCase(Locale.ROOT);
            for (List<?> n : names) {
                if (((Integer) n.get(0)) != 2) {
                    continue;
                }
                String pattern = ((String) n.get(1)).toLowerCase(Locale.ROOT);
                if (pattern.equals(h)) {
                    return true;
                }
                if (pattern.startsWith("*.") && h.indexOf('.') > 0 && h.substring(h.indexOf('.')).equals(pattern.substring(1))) {
                    return true;
                }
            }
            return false;
        } catch (java.security.cert.CertificateParsingException e) {
            return false;
        }
    }

    /** The certificate was not accepted. */
    public static final class Untrusted extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public Untrusted(String reason) {
            super(reason);
        }
    }
}
