package com.jk.explore.security.certs;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The chain check worked out by hand, one certificate at a time, so the demo can show each step that the JDK's PKIX validator hides.
 * Each certificate must name the next one as its issuer, carry a signature that the next one's public key checks, be inside its dates,
 * not be revoked, and, above the server's certificate, be allowed to issue certificates. The last one is checked against a trusted root.
 * A test proves it reaches the same verdict as {@link ChainValidator} for every chain in the demo.
 */
final class ChainSteps {

    /** The checks on one certificate. {@code mayIssue} is true for the server's own certificate, which does not need to issue. */
    record Step(X509Certificate certificate, X509Certificate next, boolean issuerNamed, boolean signatureChecks,
                boolean inDates, boolean mayIssue, boolean notRevoked) {

        boolean passes() {
            return issuerNamed && signatureChecks && inDates && mayIssue && notRevoked;
        }

        /** The first check that failed, in the order a validator makes them, or "none". */
        String firstFailure() {
            return !issuerNamed ? "the issuer's name" : !signatureChecks ? "the signature" : !inDates ? "the dates"
                    : !mayIssue ? "the CA flag" : !notRevoked ? "revoked" : "none";
        }
    }

    private ChainSteps() {
    }

    /** One step per certificate in the chain: server first, then intermediates; the root is the trusted one. */
    static List<Step> walk(List<X509Certificate> chain, X509Certificate root, Set<BigInteger> revoked, Instant when) {
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            X509Certificate certificate = chain.get(i);
            X509Certificate next = i + 1 < chain.size() ? chain.get(i + 1) : root;   // who should have signed it
            steps.add(new Step(certificate, next,
                    certificate.getIssuerX500Principal().equals(next.getSubjectX500Principal()),
                    signedBy(certificate, next),
                    !when.isBefore(certificate.getNotBefore().toInstant()) && !when.isAfter(certificate.getNotAfter().toInstant()),
                    i == 0 || certificate.getBasicConstraints() >= 0,                  // above the server: a CA
                    !revoked.contains(certificate.getSerialNumber())));
        }
        return steps;
    }

    static boolean accepted(List<Step> steps) {
        return steps.stream().allMatch(Step::passes);
    }

    /** The first failed check in the chain, or "none". */
    static String firstFailure(List<Step> steps) {
        return steps.stream().filter(step -> !step.passes()).map(Step::firstFailure).findFirst().orElse("none");
    }

    static boolean signedBy(X509Certificate certificate, X509Certificate issuer) {
        try {
            certificate.verify(issuer.getPublicKey());                                 // the issuer's public key checks the signature
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    static LocalDate day(java.util.Date date) {
        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
