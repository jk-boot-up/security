package com.jk.explore.security.certs;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

/** Validates a certificate chain with the JDK's PKIX implementation, on a date the caller chooses, against the roots the caller trusts. */
final class ChainValidator {

    /** Whether the chain was accepted and, if not, the JDK's reason for refusing it. */
    record Result(boolean accepted, String reason) {

        static Result ok() {
            return new Result(true, "accepted");
        }
    }

    private final Set<X509Certificate> trustedRoots;
    private final Set<BigInteger> revokedSerials;

    ChainValidator(Set<X509Certificate> trustedRoots, Set<BigInteger> revokedSerials) {
        this.trustedRoots = Set.copyOf(trustedRoots);
        this.revokedSerials = Set.copyOf(revokedSerials);
    }

    /** The chain is the server's certificate first, then the intermediates; the trusted root is not part of it. */
    Result validate(List<X509Certificate> chain, Instant when) {
        try {
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            PKIXParameters parameters = new PKIXParameters(trustedRoots.stream().map(root -> new TrustAnchor(root, null)).collect(java.util.stream.Collectors.toSet()));
            parameters.setRevocationEnabled(false);
            parameters.setDate(Date.from(when));
            parameters.addCertPathChecker(new RevocationList(revokedSerials));
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
            return Result.ok();
        } catch (CertPathValidatorException e) {
            return new Result(false, String.valueOf(e.getReason()));
        } catch (GeneralSecurityException e) {
            return new Result(false, "ERROR " + e.getClass().getSimpleName());
        }
    }
}
