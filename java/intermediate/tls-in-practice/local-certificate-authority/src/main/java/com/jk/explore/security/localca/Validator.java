package com.jk.explore.security.localca;

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

/**
 * What every client of the store's services does: validate the chain with the JDK's PKIX implementation against the one internal root,
 * at a chosen time, optionally requiring a current revocation list from every issuer in the chain. With revocation on and no current list,
 * the JDK refuses: it fails closed.
 */
final class Validator {

    record Result(boolean accepted, String reason) {

        @Override
        public String toString() {
            return accepted ? "accepted" : "refused (" + reason + ")";
        }
    }

    private final X509Certificate root;

    Validator(X509Certificate root) {
        this.root = root;
    }

    Result check(List<X509Certificate> chain, Instant when) {
        return check(chain, when, false, List.of());
    }

    Result checkWithLists(List<X509Certificate> chain, Instant when, Collection<X509CRL> lists) {
        return check(chain, when, true, lists);
    }

    private Result check(List<X509Certificate> chain, Instant when, boolean revocation, Collection<X509CRL> lists) {
        try {
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(root, null)));
            parameters.setDate(Date.from(when));
            parameters.setRevocationEnabled(revocation);
            if (revocation) {
                parameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(new ArrayList<>(lists))));
            }
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
            return new Result(true, "accepted");
        } catch (CertPathValidatorException e) {
            return new Result(false, String.valueOf(e.getReason()));
        } catch (GeneralSecurityException e) {
            return new Result(false, e.getClass().getSimpleName());
        }
    }
}
