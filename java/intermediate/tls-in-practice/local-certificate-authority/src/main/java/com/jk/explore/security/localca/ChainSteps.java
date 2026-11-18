package com.jk.explore.security.localca;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

/**
 * A teaching copy of what a client does with the chain leaf, issuing authority, root: the same checks the JDK's PKIX validator
 * makes, done by hand one at a time so each can be printed. A test proves it reaches the validator's exact verdict.
 */
final class ChainSteps {

    record Step(String text, boolean ok, String reason) {
    }

    private ChainSteps() {
    }

    static String cn(X509Certificate certificate) {
        return cn(certificate.getSubjectX500Principal().getName());
    }

    /** The common name (CN) out of a distinguished name such as {@code O=Example Shop,CN=Shop Issuing CA}. */
    static String cn(String name) {
        for (String part : name.split(",")) {
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return name;
    }

    /**
     * The checks, in the order the JDK makes them: from the trusted root down. For each certificate below the root, the limits its issuer
     * set (name constraint, path length) come first, then its signature and its dates. Every step is made, so a picture can show all.
     */
    static List<Step> walk(X509Certificate leaf, X509Certificate issuing, X509Certificate trustedRoot, Instant when) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(cn(trustedRoot) + " is the client's trusted root",
                issuing.getIssuerX500Principal().equals(trustedRoot.getSubjectX500Principal()), "NO_TRUST_ANCHOR"));
        steps.add(new Step(cn(issuing) + " is signed by " + cn(trustedRoot), signedBy(issuing, trustedRoot), "INVALID_SIGNATURE"));
        String issuingDates = validity(when, issuing);
        steps.add(new Step(cn(issuing) + " is valid on that date", issuingDates.isEmpty(), issuingDates));
        List<String> names = names(leaf);
        String permitted = permittedDomain(issuing);
        boolean inside = names.stream()
                .allMatch(n -> permitted == null || n.toLowerCase(Locale.ROOT).endsWith("." + permitted));
        steps.add(new Step(String.join(", ", names) + " is inside " + permitted,
                inside, "INVALID_NAME"));
        steps.add(new Step(String.join(", ", names) + " is not an authority (path length " + issuing.getBasicConstraints() + ")",
                leaf.getBasicConstraints() == -1, "PATH_TOO_LONG"));
        steps.add(new Step(String.join(", ", names) + " is signed by " + cn(issuing), signedBy(leaf, issuing), "INVALID_SIGNATURE"));
        String leafDates = validity(when, leaf);
        steps.add(new Step(String.join(", ", names) + " is valid on that date", leafDates.isEmpty(), leafDates));
        return steps;
    }

    /** The verdict, written the way {@link Validator.Result} writes it: the first failed step decides. */
    static String verdict(List<Step> steps) {
        for (Step step : steps) {
            if (!step.ok()) {
                return "refused (" + step.reason() + ")";
            }
        }
        return "accepted";
    }

    private static boolean signedBy(X509Certificate subject, X509Certificate issuer) {
        try {
            subject.verify(issuer.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Empty when the date is inside the certificate's validity period, otherwise the reason the JDK gives. */
    private static String validity(Instant when, X509Certificate certificate) {
        try {
            certificate.checkValidity(Date.from(when));
            return "";
        } catch (CertificateExpiredException e) {
            return "EXPIRED";
        } catch (CertificateNotYetValidException e) {
            return "NOT_YET_VALID";
        }
    }

    static List<String> names(X509Certificate certificate) {
        List<String> names = new ArrayList<>();
        try {
            for (List<?> entry : certificate.getSubjectAlternativeNames()) {
                names.add(String.valueOf(entry.get(1)));
            }
        } catch (CertificateParsingException e) {
            throw new IllegalStateException(e);
        }
        return names;
    }

    /** The DNS name the issuing authority's name constraint permits, or null when it has none. */
    static String permittedDomain(X509Certificate issuing) {
        byte[] value = issuing.getExtensionValue(Extension.nameConstraints.getId());
        if (value == null) {
            return null;
        }
        try {
            NameConstraints constraints = NameConstraints.getInstance(JcaX509ExtensionUtils.parseExtensionValue(value));
            GeneralSubtree first = constraints.getPermittedSubtrees()[0];
            return String.valueOf(first.getBase().getName());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(new CertificateException(e));
        }
    }
}
