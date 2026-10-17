package com.jk.explore.security.certs;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/** A certificate together with the key pair of its subject. Whoever holds the private key can issue certificates in this name. */
record Issued(KeyPair keys, X509Certificate certificate) {
}
