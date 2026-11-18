package com.jk.explore.security.tlsservers;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/** The two checks a client makes on the certificate a server sends, redone by hand: who signed it, and whom it names. */
final class ChainCheck {

    /** {@code signer}: the name at the top of the chain; {@code trusted}: one of the client's roots signed it; {@code listed}: the name asked is in the certificate. */
    record Verdict(String signer, boolean trusted, String name, boolean listed) {

        boolean ok() {
            return trusted && listed;
        }
    }

    private ChainCheck() {
    }

    /** Walks the chain from the server's certificate up; the last certificate must be signed by one of {@code roots}. */
    static Verdict check(List<X509Certificate> chain, Collection<X509Certificate> roots, String name) {
        boolean signed = true;
        for (int i = 0; i + 1 < chain.size(); i++) {
            signed &= signedBy(chain.get(i), chain.get(i + 1));
        }
        X509Certificate top = chain.get(chain.size() - 1);
        boolean trusted = false;
        X500Principal issuer = top.getIssuerX500Principal();
        for (X509Certificate root : roots) {
            boolean named = root.getSubjectX500Principal().equals(issuer);
            if (named && signedBy(top, root)) {
                trusted = true;
            }
        }
        boolean listed = names(chain.get(0)).contains(name);
        return new Verdict(commonName(issuer.getName()), signed && trusted, name, listed);
    }

    /** True when {@code issuer}'s public key verifies {@code cert}'s signature. */
    static boolean signedBy(X509Certificate cert, X509Certificate issuer) {
        try {
            cert.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** The DNS names in the certificate's Subject Alternative Name extension. */
    static List<String> names(X509Certificate cert) {
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            return sans == null ? List.of() : sans.stream().filter(s -> (Integer) s.get(0) == 2).map(s -> (String) s.get(1)).toList();
        } catch (GeneralSecurityException e) {
            return List.of();
        }
    }

    /** True when the certificate's Authority Information Access extension is present, which is where an OCSP responder URL would be. */
    static boolean namesAResponder(X509Certificate cert) {
        return cert.getExtensionValue("1.3.6.1.5.5.7.1.1") != null;
    }

    static String commonName(String distinguishedName) {
        return distinguishedName.replaceAll("^CN=([^,]*).*", "$1");
    }

    /** The roots the JDK trusts when a client brings none of its own. */
    static List<X509Certificate> defaultRoots() throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return Arrays.stream(factory.getTrustManagers()).filter(X509TrustManager.class::isInstance)
                .flatMap(t -> Arrays.stream(((X509TrustManager) t).getAcceptedIssuers())).toList();
    }
}
