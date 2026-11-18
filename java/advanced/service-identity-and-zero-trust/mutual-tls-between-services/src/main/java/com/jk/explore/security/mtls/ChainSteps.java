package com.jk.explore.security.mtls;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * A teaching class: the chain check the ledger's TLS makes on a caller's certificates, redone by hand. Each certificate must name the next one as its
 * issuer and carry a signature that the next one's public key verifies; the last one must be signed by a root in the trust file. A test proves the answer
 * is the same as the JDK's own check.
 */
final class ChainSteps {

    /** One link: who, who says it issued it, the certificate whose key was used to check it (empty when none fits), and whether the signature verifies. */
    record Link(String subject, String issuer, String checkedWith, boolean verifies) {
    }

    private ChainSteps() {
    }

    static List<Link> walk(List<X509Certificate> chain, List<X509Certificate> roots) {
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            X509Certificate c = chain.get(i);
            X509Certificate next = i + 1 < chain.size() ? chain.get(i + 1) : roots.stream()
                    .filter(r -> r.getSubjectX500Principal().equals(c.getIssuerX500Principal())).findFirst().orElse(null);
            boolean nameFits = next != null && next.getSubjectX500Principal().equals(c.getIssuerX500Principal());
            links.add(new Link(cn(c.getSubjectX500Principal().getName()), cn(c.getIssuerX500Principal().getName()),
                    nameFits ? cn(next.getSubjectX500Principal().getName()) : "", nameFits && verifies(c, next.getPublicKey())));
        }
        return links;
    }

    /** True when every link verifies and the last one was checked with a root of the trust file. */
    static boolean trusted(List<X509Certificate> chain, List<X509Certificate> roots) {
        List<Link> links = walk(chain, roots);
        return links.stream().allMatch(Link::verifies) && !links.isEmpty() && !links.getLast().checkedWith().isEmpty();
    }

    private static boolean verifies(X509Certificate c, PublicKey key) {
        try {
            c.verify(key);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** The common name of a distinguished name, for example "Bank Internal CA Root CA" from "CN=Bank Internal CA Root CA,O=Bank Internal CA". */
    static String cn(String dn) {
        for (String part : dn.split(",")) {
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return dn;
    }
}
