package com.jk.explore.security.certs;

import java.math.BigInteger;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

/**
 * An explicit list of revoked serial numbers, plugged into the JDK's path validation as an extra check. Real revocation uses signed lists
 * (CRLs) or an online service (OCSP) that the authority publishes; this list stands for their answer so the idea can be shown with no network.
 */
final class RevocationList extends PKIXCertPathChecker {

    private final Set<BigInteger> revoked;

    RevocationList(Set<BigInteger> revoked) {
        this.revoked = Set.copyOf(revoked);
    }

    @Override
    public void init(boolean forward) {
        // nothing to prepare: the list is fixed
    }

    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return null;
    }

    @Override
    public void check(Certificate certificate, Collection<String> unresolvedCritical) throws CertPathValidatorException {
        if (certificate instanceof X509Certificate x509 && revoked.contains(x509.getSerialNumber())) {
            throw new CertPathValidatorException("certificate " + x509.getSerialNumber() + " is revoked", null, null, -1,
                    CertPathValidatorException.BasicReason.REVOKED);
        }
    }
}
