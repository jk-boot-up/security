package com.jk.explore.security.sbomscan;

/**
 * A teaching class: a package URL split into its parts. pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1 is the type maven, the namespace (the Maven group)
 * org.apache.logging.log4j, the name log4j-core and the version 2.14.1. A scanner matches advisories on exactly these parts.
 */
record Purl(String type, String namespace, String name, String version) {

    static Purl parse(String purl) {
        if (!purl.startsWith("pkg:") || purl.indexOf('@') < 0) {
            throw new IllegalArgumentException("not a package URL with a version: " + purl);
        }
        String rest = purl.substring("pkg:".length());
        String type = rest.substring(0, rest.indexOf('/'));
        String path = rest.substring(type.length() + 1, rest.lastIndexOf('@'));
        String version = rest.substring(rest.lastIndexOf('@') + 1).replaceAll("[?#].*$", "");
        int slash = path.lastIndexOf('/');
        return new Purl(type, slash < 0 ? "" : path.substring(0, slash), path.substring(slash + 1), version);
    }

    /** The short form used on screen: name@version. */
    String shortName() {
        return name + "@" + version;
    }
}
