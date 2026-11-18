package com.jk.explore.security.pinning;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The two checks of a pinning client, redone by hand: which device root signed the top of the chain, and which key of the chain is pinned. */
final class ChainWalk {

    private ChainWalk() {
    }

    /** The common name in a certificate's subject or issuer. */
    static String name(javax.security.auth.x500.X500Principal p) {
        return p.getName().replaceFirst("^CN=", "");
    }

    /** The device root whose name is the issuer of the chain's top certificate and whose key verifies its signature. */
    static Optional<X509Certificate> anchor(List<X509Certificate> chain, Set<X509Certificate> roots) {
        X509Certificate top = chain.get(chain.size() - 1);
        for (X509Certificate root : roots) {
            boolean named = root.getSubjectX500Principal().equals(top.getIssuerX500Principal());
            if (named && signedBy(top, root)) {
                return Optional.of(root);
            }
        }
        return Optional.empty();
    }

    private static boolean signedBy(X509Certificate cert, X509Certificate issuer) {
        try {
            cert.verify(issuer.getPublicKey());
            return true;
        } catch (java.security.GeneralSecurityException e) {
            return false;
        }
    }

    /** The first certificate of the chain whose key's pin is in {@code pins}. */
    static Optional<X509Certificate> pinned(List<X509Certificate> chain, Set<String> pins) {
        return chain.stream().filter(c -> pins.contains(Spki.pin(c))).findFirst();
    }
}
