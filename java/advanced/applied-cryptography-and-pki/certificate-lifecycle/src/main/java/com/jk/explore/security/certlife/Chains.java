package com.jk.explore.security.certlife;

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Chain building and validation with the JDK's PKIX provider, with a clock we choose and revocation checking we configure. */
final class Chains {

    /** What a client does when it cannot learn the revocation status: refuse (hard fail) or accept (soft fail). */
    enum Policy {
        HARD(EnumSet.noneOf(Option.class)),
        SOFT(EnumSet.of(Option.SOFT_FAIL));

        final Set<Option> options;

        Policy(Set<Option> options) {
            this.options = options;
        }
    }

    /** How revocation is checked. */
    enum Source {
        NONE, OCSP, CRL
    }

    private Chains() {
    }

    /**
     * Validates the leaf against the root, with the given intermediates, at the given time. Revocation comes from the OCSP responder named in the
     * certificate (or the stapled responses) or from the given CRLs only.
     */
    static Verdict validate(X509Certificate leaf, List<X509Certificate> intermediates, X509Certificate root, Instant when, Source source,
            Policy policy, List<X509CRL> crls, Map<X509Certificate, byte[]> stapled) {
        try {
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(leaf);
            Set<TrustAnchor> anchors = Set.of(new TrustAnchor(root, null));
            PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, selector);
            List<Object> store = new ArrayList<>(intermediates);
            store.addAll(crls);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(store)));
            params.setRevocationEnabled(false);
            // Step one: is there a chain from this certificate to the root at all? (asked at a moment when every certificate is inside its dates)
            params.setDate(newestStart(leaf, intermediates, root));
            CertPath path;
            try {
                path = CertPathBuilder.getInstance("PKIX").build(params).getCertPath();
            } catch (CertPathBuilderException e) {
                return new Verdict(false, "no chain to the root");
            }
            // Step two: the chain at the time we choose, with the revocation check we configure; this step says why it failed.
            params.setDate(Date.from(when));
            if (source != Source.NONE) {
                PKIXRevocationChecker checker = (PKIXRevocationChecker) CertPathValidator.getInstance("PKIX").getRevocationChecker();
                Set<Option> options = EnumSet.of(Option.ONLY_END_ENTITY, Option.NO_FALLBACK);
                options.addAll(policy.options);
                if (source == Source.CRL) {
                    options.add(Option.PREFER_CRLS);
                }
                checker.setOptions(options);
                if (!stapled.isEmpty()) {
                    checker.setOcspResponses(stapled);
                }
                params.addCertPathChecker(checker);
            }
            CertPathValidator.getInstance("PKIX").validate(path, params);
            return Verdict.OK;
        } catch (CertPathValidatorException e) {
            return new Verdict(false, reason(e));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static Verdict validate(X509Certificate leaf, List<X509Certificate> intermediates, X509Certificate root, Instant when, Source source, Policy policy,
            List<X509CRL> crls) {
        return validate(leaf, intermediates, root, when, source, policy, crls, Map.of());
    }

    private static Date newestStart(X509Certificate leaf, List<X509Certificate> intermediates, X509Certificate root) {
        Date newest = Date.from(Instant.EPOCH);
        for (X509Certificate c : Stream.concat(Stream.of(leaf, root), intermediates.stream()).toList()) {
            if (c.getNotBefore().after(newest)) {
                newest = c.getNotBefore();
            }
        }
        return new Date(newest.getTime() + 1000);
    }

    /** The first reason the JDK gives, in words that do not change from one JDK to the next. */
    static String reason(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof CertPathValidatorException c) {
                if (c.getReason() == BasicReason.EXPIRED) {
                    return "expired";
                } else if (c.getReason() == BasicReason.NOT_YET_VALID) {
                    return "not yet valid";
                } else if (c.getReason() == BasicReason.REVOKED) {
                    return "revoked";
                } else if (c.getReason() == BasicReason.UNDETERMINED_REVOCATION_STATUS) {
                    return "revocation status unknown";
                } else if (c.getReason() == BasicReason.INVALID_SIGNATURE) {
                    return "bad signature";
                } else if (c.getMessage() != null && c.getMessage().contains("out-of-date")) {
                    return "OCSP response out of date";
                }
            }
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.security.cert.CertificateExpiredException) {
                return "expired";
            }
        }
        return "no chain to the root";
    }
}
