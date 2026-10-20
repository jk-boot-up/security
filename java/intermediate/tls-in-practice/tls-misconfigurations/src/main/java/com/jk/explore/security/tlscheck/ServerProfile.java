package com.jk.explore.security.tlscheck;

import java.util.List;
import java.util.Set;

/** Everything a scan of one server found. In this project the profiles are fixed data, so every run is the same. */
public record ServerProfile(String host, Set<String> protocols, List<String> cipherSuites, boolean serverChoosesCipher, boolean compression,
                            boolean secureRenegotiation, CertFacts certificate, HttpFacts http) {
}
