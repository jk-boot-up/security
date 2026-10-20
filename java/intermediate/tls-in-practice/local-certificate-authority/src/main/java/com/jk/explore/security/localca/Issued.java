package com.jk.explore.security.localca;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/** A certificate together with its subject's key pair. Whoever holds the private key can act, or issue, in this name. */
record Issued(KeyPair keys, X509Certificate certificate) {
}
