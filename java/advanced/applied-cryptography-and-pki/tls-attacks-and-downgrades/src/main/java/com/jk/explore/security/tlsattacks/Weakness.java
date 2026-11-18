package com.jk.explore.security.tlsattacks;

import java.util.List;

/** The weaknesses planted in the weak server: the truth the probes and testssl.sh are scored against. */
enum Weakness {
    TLS_1_0("TLS 1.0 accepted", "TLS 1.0"),
    TLS_1_1("TLS 1.1 accepted", "TLS 1.1"),
    STATIC_RSA("static RSA key exchange (no forward secrecy)", "static RSA"),
    CBC_SHA1("CBC suites with SHA-1", "CBC, SHA-1"),
    SMALL_DH("1024-bit DH group", "DH 1024 bits"),
    NULL_CIPHER("NULL cipher (no encryption)", "NULL cipher");

    static final List<Weakness> PLANTED = List.of(values());

    private final String label;
    private final String shortName;

    Weakness(String label, String shortName) {
        this.label = label;
        this.shortName = shortName;
    }

    /** A name short enough for a table column. */
    String shortName() {
        return shortName;
    }

    String label() {
        return label;
    }
}
