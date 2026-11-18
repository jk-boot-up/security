package com.jk.explore.security.mtls;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Reads the identity of a service from a certificate: the URI in the subject alternative name, never the subject name. */
final class Identities {

    static final String PREFIX = "spiffe://bank/";
    private static final int URI = 6;

    private Identities() {
    }

    static Optional<String> of(X509Certificate certificate) {
        try {
            return fromNames(certificate.getSubjectAlternativeNames());
        } catch (CertificateParsingException e) {
            return Optional.empty();
        }
    }

    /** The names as the JDK lists them: pairs of a type number and a value. */
    static Optional<String> fromNames(Collection<List<?>> names) {
        if (names == null) {
            return Optional.empty();
        }
        return names.stream()
                .filter(n -> n.size() == 2 && n.get(0) instanceof Integer type && type == URI && n.get(1) instanceof String)
                .map(n -> (String) n.get(1))
                .filter(uri -> uri.startsWith(PREFIX) && uri.length() > PREFIX.length() && uri.indexOf('/', PREFIX.length()) < 0)
                .map(uri -> uri.substring(PREFIX.length()))
                .findFirst();
    }

    /** Each subject alternative name as "URI value", "DNS value" or "IP value", in the order of the certificate. */
    static List<String> names(X509Certificate certificate) {
        try {
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            if (names == null) {
                return List.of();
            }
            return names.stream().map(n -> switch ((Integer) n.get(0)) {
                case 2 -> "DNS " + n.get(1);
                case 6 -> "URI " + n.get(1);
                case 7 -> "IP " + n.get(1);
                default -> "type " + n.get(0) + " " + n.get(1);
            }).toList();
        } catch (CertificateParsingException e) {
            return List.of();
        }
    }
}
