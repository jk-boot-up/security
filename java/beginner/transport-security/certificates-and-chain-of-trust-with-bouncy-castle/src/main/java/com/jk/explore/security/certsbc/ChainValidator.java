package com.jk.explore.security.certsbc;

import java.security.GeneralSecurityException;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Validates a certificate chain with the JDK's PKIX implementation, on a date the caller chooses, against the roots the caller trusts, optionally checking revocation lists. */
final class ChainValidator {

    /** Whether the chain was accepted and, if not, the JDK's reason for refusing it. */
    record Result(boolean accepted, String reason) {

        static Result ok() {
            return new Result(true, "accepted");
        }
    }

    private final Set<X509Certificate> trustedRoots;

    ChainValidator(Set<X509Certificate> trustedRoots) {
        this.trustedRoots = Set.copyOf(trustedRoots);
    }

    /** Validates without revocation checking. The chain is the server's certificate first, then the intermediates; the trusted root is not part of it. */
    Result validate(List<X509Certificate> chain, Instant when) {
        return validate(chain, when, false, List.of());
    }

    /** Validates with revocation checking on: every certificate in the chain needs a current list from its issuer, or the chain is refused. */
    Result validateWithLists(List<X509Certificate> chain, Instant when, Collection<X509CRL> lists) {
        return validate(chain, when, true, lists);
    }

    private Result validate(List<X509Certificate> chain, Instant when, boolean revocation, Collection<X509CRL> lists) {
        try {
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            PKIXParameters parameters = new PKIXParameters(trustedRoots.stream().map(root -> new TrustAnchor(root, null)).collect(Collectors.toSet()));
            parameters.setRevocationEnabled(revocation);
            parameters.setDate(Date.from(when));
            if (revocation) {
                parameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(new ArrayList<>(lists))));
            }
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
            return Result.ok();
        } catch (CertPathValidatorException e) {
            return new Result(false, String.valueOf(e.getReason()));
        } catch (GeneralSecurityException e) {
            return new Result(false, "ERROR " + e.getClass().getSimpleName());
        }
    }
}
