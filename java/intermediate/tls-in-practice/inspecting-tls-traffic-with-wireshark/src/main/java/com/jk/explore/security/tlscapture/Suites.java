package com.jk.explore.security.tlscapture;

import java.util.Map;

/** Cipher suite names, and what a name says about the key exchange and the size of a secret. */
final class Suites {

    private static final Map<String, String> NAMES = Map.of(
            "0x1301", "TLS_AES_128_GCM_SHA256",
            "0x1302", "TLS_AES_256_GCM_SHA384",
            "0x1303", "TLS_CHACHA20_POLY1305_SHA256",
            "0xc02f", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "0xc030", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "0x002f", "TLS_RSA_WITH_AES_128_CBC_SHA",
            "0x0035", "TLS_RSA_WITH_AES_256_CBC_SHA");

    private Suites() {
    }

    /** The name of the suite tshark prints as a number, such as 0x1302. */
    static String name(String code) {
        return NAMES.getOrDefault(code, code);
    }

    /** TLS 1.3 names carry no key exchange: it is always ephemeral. TLS 1.2 names say it after TLS_. */
    static String keyExchange(String name) {
        if (!name.contains("_WITH_")) {
            return "ephemeral: a new key pair per connection";
        }
        if (name.startsWith("TLS_ECDHE_") || name.startsWith("TLS_DHE_")) {
            return "ephemeral (ECDHE): the server key only signs";
        }
        return "RSA: the secret is sent locked to the server key";
    }

    /** A leaked server key opens a recording only when the secret itself crossed the wire, locked to that key. */
    static boolean serverKeyOpens(String name) {
        return name.startsWith("TLS_RSA_WITH_");
    }

    /** The hash at the end of a TLS 1.3 name, such as SHA384. */
    static String hash(String name) {
        return name.substring(name.lastIndexOf('_') + 1);
    }

    /** A TLS 1.3 secret is as long as the suite's hash: SHA384 gives 384 / 8 = 48 bytes. */
    static int secretBytes(String name) {
        return Integer.parseInt(hash(name).substring(3)) / 8;
    }
}
