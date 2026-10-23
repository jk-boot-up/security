package com.jk.explore.security.tlsattacks;

import java.util.List;

/** The weaknesses planted in the weak server: the truth the probes and testssl.sh are scored against. */
enum Weakness {
    TLS_1_0("TLS 1.0 accepted"),
    TLS_1_1("TLS 1.1 accepted"),
    STATIC_RSA("static RSA key exchange (no forward secrecy)"),
    CBC_SHA1("CBC suites with SHA-1"),
    SMALL_DH("1024-bit DH group"),
    NULL_CIPHER("NULL cipher (no encryption)");

    static final List<Weakness> PLANTED = List.of(values());

    private final String label;

    Weakness(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
