package com.jk.explore.security.pinning;

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A mobile-style client. First it validates the chain like any client against the roots its device trusts (and a device can trust more roots than the app's
 * author expected: a company proxy, malware, a debugging tool). Then, if it has a pin policy, it also requires that a key in the chain is pinned.
 */
public final class PinnedClient {

    private final Set<X509Certificate> deviceRoots;
    private final PinPolicy policy;
    private final List<String> reports = new ArrayList<>();

    /** {@code policy} may be null: an app that does not pin. */
    public PinnedClient(Set<X509Certificate> deviceRoots, PinPolicy policy) {
        this.deviceRoots = deviceRoots;
        this.policy = policy;
    }

    public List<String> reports() {
        return reports;
    }

    /** {@code chain} is what the server sent, leaf first, without the root. */
    public Outcome connect(List<X509Certificate> chain, String host, Instant now) {
        if (!validates(chain, host, now)) {
            return Outcome.REFUSED_BY_VALIDATION;
        }
        if (policy == null) {
            return Outcome.ACCEPTED;
        }
        boolean pinned = chain.stream().anyMatch(c -> policy.pins().contains(Spki.pin(c)));
        if (pinned) {
            return Outcome.ACCEPTED;
        }
        reports.add("pin mismatch for " + host + ": chain keys " + chain.stream().map(c -> Spki.pin(c).substring(0, 8)).toList());
        return policy.reportOnly() ? Outcome.ACCEPTED_REPORT_ONLY : Outcome.REFUSED_BY_PIN;
    }

    private boolean validates(List<X509Certificate> chain, String host, Instant now) {
        try {
            Set<TrustAnchor> anchors = new java.util.HashSet<>();
            deviceRoots.forEach(r -> anchors.add(new TrustAnchor(r, null)));
            PKIXParameters params = new PKIXParameters(anchors);
            params.setRevocationEnabled(false); // revocation is the local-certificate-authority project; this one is about pins
            params.setDate(Date.from(now));
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(chain);
            CertPathValidator.getInstance("PKIX").validate(path, params);
            return nameMatches(chain.get(0), host);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    static boolean nameMatches(X509Certificate leaf, String host) {
        try {
            var names = leaf.getSubjectAlternativeNames();
            if (names == null) {
                return false;
            }
            for (List<?> n : names) {
                if ((Integer) n.get(0) == 2 && ((String) n.get(1)).toLowerCase(Locale.ROOT).equals(host.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        } catch (java.security.cert.CertificateParsingException e) {
            return false;
        }
        return false;
    }
}
