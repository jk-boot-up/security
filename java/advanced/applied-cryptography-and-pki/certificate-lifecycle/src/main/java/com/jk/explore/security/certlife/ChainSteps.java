package com.jk.explore.security.certlife;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * A teaching class: the chain check that the JDK's PKIX validator does, redone step by step (RFC 5280, section 6, the parts this demo needs).
 * The demo uses Chains (the JDK); a test proves that these steps accept and refuse the same chains.
 */
final class ChainSteps {

    /** One step: what was checked, and whether it held. */
    record Step(String what, boolean ok) {
    }

    private ChainSteps() {
    }

    static List<Step> check(X509Certificate leaf, List<X509Certificate> intermediates, X509Certificate root, Instant when) {
        List<Step> steps = new ArrayList<>();
        Optional<X509Certificate> found = intermediates.stream().filter(c -> c.getSubjectX500Principal().equals(leaf.getIssuerX500Principal())).findFirst();
        steps.add(new Step("leaf issuer = intermediate subject", found.isPresent()));
        if (found.isEmpty()) {
            return steps;
        }
        X509Certificate inter = found.get();
        steps.add(new Step("leaf signature, intermediate's key", signedBy(leaf, inter)));
        steps.add(new Step("intermediate issuer = root subject", inter.getIssuerX500Principal().equals(root.getSubjectX500Principal())));
        steps.add(new Step("intermediate signature, root's key", signedBy(inter, root)));
        steps.add(new Step("root: self-signed, held by the client", signedBy(root, root)));
        // a CA (basic constraints) allowed to sign (keyCertSign), and path length 0: no CA may sit below it
        boolean[] ku = inter.getKeyUsage();
        steps.add(new Step("intermediate: a CA, keyCertSign, path 0", inter.getBasicConstraints() >= 0 && ku != null && ku[5] && leaf.getBasicConstraints() < 0));
        Date at = Date.from(when);
        steps.add(new Step("the time is inside every validity", List.of(leaf, inter, root).stream()
                .allMatch(c -> !at.before(c.getNotBefore()) && !at.after(c.getNotAfter()))));
        return steps;
    }

    static boolean accepted(List<Step> steps) {
        return steps.size() == 7 && steps.stream().allMatch(Step::ok);
    }

    /** The number of the first step that failed, or 0 when all held. */
    static int firstFailure(List<Step> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).ok()) {
                return i + 1;
            }
        }
        return 0;
    }

    private static boolean signedBy(X509Certificate c, X509Certificate issuer) {
        try {
            c.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
