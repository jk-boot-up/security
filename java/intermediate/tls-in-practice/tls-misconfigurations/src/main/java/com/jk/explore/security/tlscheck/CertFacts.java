package com.jk.explore.security.tlscheck;

import java.time.Instant;
import java.util.List;

/**
 * What a scan learns about a server's certificate chain, in the terms a checker needs. {@code chainComplete} is false when the server sends only its own
 * certificate and the client has to find the intermediate by itself (browsers sometimes can; most programs cannot).
 */
public record CertFacts(List<String> dnsNames, Instant notBefore, Instant notAfter, String keyType, int keyBits, String signature,
                        boolean selfSigned, boolean chainComplete, boolean trustedRoot) {
}
