package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/** Finds the planted weaknesses of a server by connecting to it with openssl s_client (our own probes). */
final class Audit {

    private Audit() {
    }

    /** The protocol versions and cipher suites probed, in the order they are printed. */
    static final String[] VERSIONS = {"-tls1", "-tls1_1", "-tls1_2", "-tls1_3"};
    static final String[] VERSION_NAMES = {"TLSv1.0", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

    /** A fixed set of TLS 1.2 suites: name, what it is. */
    static final String[][] SUITES = {
        {"AES128-GCM-SHA256", "static RSA, GCM"},
        {"AES128-SHA", "static RSA, CBC, SHA-1"},
        {"DHE-RSA-AES128-GCM-SHA256", "finite-field DH, GCM"},
        {"DHE-RSA-AES128-SHA", "finite-field DH, CBC, SHA-1"},
        {"ECDHE-RSA-AES128-SHA", "elliptic curve DH, CBC, SHA-1"},
        {"ECDHE-RSA-AES128-GCM-SHA256", "elliptic curve DH, GCM"},
        {"ECDHE-RSA-CHACHA20-POLY1305", "elliptic curve DH, ChaCha20"},
        {"NULL-SHA", "no encryption"},
    };

    static Probe version(Openssl o, int port, int i) throws IOException, InterruptedException {
        return Probe.with(o, port, VERSIONS[i], "DEFAULT:NULL-SHA");
    }

    static Probe suite(Openssl o, int port, int i) throws IOException, InterruptedException {
        return Probe.with(o, port, "-tls1_2", SUITES[i][0]);
    }

    /** Which planted weaknesses a server shows to a client that asks for them. */
    static Set<Weakness> find(Openssl o, int port) throws IOException, InterruptedException {
        Set<Weakness> found = EnumSet.noneOf(Weakness.class);
        if (version(o, port, 0).connected()) {
            found.add(Weakness.TLS_1_0);
        }
        if (version(o, port, 1).connected()) {
            found.add(Weakness.TLS_1_1);
        }
        if (suite(o, port, 0).connected()) {
            found.add(Weakness.STATIC_RSA);
        }
        if (suite(o, port, 1).connected()) {
            found.add(Weakness.CBC_SHA1);
        }
        Probe dh = suite(o, port, 2);
        if (dh.connected() && dh.dhBits() > 0 && dh.dhBits() < 2048) {
            found.add(Weakness.SMALL_DH);
        }
        if (suite(o, port, 7).connected()) {
            found.add(Weakness.NULL_CIPHER);
        }
        return found;
    }
}
