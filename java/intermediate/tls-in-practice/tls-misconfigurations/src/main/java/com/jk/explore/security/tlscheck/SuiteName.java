package com.jk.explore.security.tlscheck;

/**
 * A TLS 1.2 cipher suite name split into its parts, the way {@link CipherSuites} reads it: TLS_ key exchange _ authentication _WITH_ cipher _ mode _ hash.
 * A teaching class: it shows where the words that the rules look for sit in the name.
 */
public record SuiteName(String keyExchange, String authentication, String cipher, String mode, String hash) {

    public static SuiteName parse(String suite) {
        if (CipherSuites.tls13(suite)) {
            throw new IllegalArgumentException("a TLS 1.3 name has no key exchange part: " + suite);
        }
        String[] sides = suite.substring("TLS_".length()).split("_WITH_");
        String[] kx = sides[0].split("_", 2);
        String body = sides[1];
        String hash = body.substring(body.lastIndexOf('_') + 1);
        String bulk = body.substring(0, body.lastIndexOf('_'));
        String mode = bulk.endsWith("_GCM") ? "GCM" : bulk.endsWith("_CBC") ? "CBC" : bulk.startsWith("CHACHA20") ? "POLY1305" : "stream";
        String cipher = bulk.endsWith("_GCM") || bulk.endsWith("_CBC") ? bulk.substring(0, bulk.length() - 4) : bulk.replace("_POLY1305", "");
        return new SuiteName(kx[0], kx.length > 1 ? kx[1] : kx[0], cipher, mode, hash);
    }
}
