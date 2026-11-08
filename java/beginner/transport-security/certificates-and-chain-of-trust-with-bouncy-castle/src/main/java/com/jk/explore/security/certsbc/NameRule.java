package com.jk.explore.security.certsbc;

/**
 * The name constraint worked out by hand: an authority limited to a domain may certify that name, or a name that ends with a dot and
 * the domain. The dot matters: evilshop.example ends with shop.example but is not under it. A test proves the JDK's PKIX check agrees.
 */
final class NameRule {

    private NameRule() {
    }

    static boolean permits(String domain, String host) {
        return host.equals(domain) || host.endsWith("." + domain);   // the domain itself, or one of its sub-names
    }

    /** Which part of the rule decides, in words, for the demo. */
    static String why(String domain, String host) {
        if (host.equals(domain)) {
            return "the same name";
        }
        if (host.endsWith("." + domain)) {
            return "ends with ." + domain;
        }
        return "neither";
    }
}
