package com.jk.explore.security.certs;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Does the certificate vouch for this host name? A valid chain proves who signed it, not that it is the certificate for the site you meant. */
final class HostName {

    private static final int DNS_NAME = 2;

    private HostName() {
    }

    static boolean matches(X509Certificate certificate, String host) {
        Collection<List<?>> names;
        try {
            names = certificate.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            return false;
        }
        if (names == null) {
            return false;
        }
        String wanted = host.toLowerCase(Locale.ROOT);
        for (List<?> entry : names) {
            if (((Integer) entry.get(0)) == DNS_NAME && matchesPattern(((String) entry.get(1)).toLowerCase(Locale.ROOT), wanted)) {
                return true;
            }
        }
        return false;
    }

    /** An exact name, or a wildcard in the leftmost label that stands for exactly one label: *.example matches shop.example, not a.b.example. */
    static boolean matchesPattern(String pattern, String host) {
        if (!pattern.startsWith("*.")) {
            return pattern.equals(host);
        }
        int dot = host.indexOf('.');
        return dot > 0 && host.substring(dot + 1).equals(pattern.substring(2));
    }
}
