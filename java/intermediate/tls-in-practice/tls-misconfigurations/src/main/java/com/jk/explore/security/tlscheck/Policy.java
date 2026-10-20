package com.jk.explore.security.tlscheck;

import java.util.List;
import java.util.Set;

/** A target configuration, after the Mozilla "modern" and "intermediate" profiles, that a server can be moved to and a config file generated from. */
public record Policy(String name, Set<String> protocols, List<String> cipherSuites) {

    public static final Policy MODERN = new Policy("modern", Set.of("TLSv1.3"),
            List.of("TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

    public static final Policy INTERMEDIATE = new Policy("intermediate", Set.of("TLSv1.2", "TLSv1.3"),
            List.of("TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256"));

    /** Applies the policy to a server: protocols and suites come from the policy, everything else is kept. */
    public ServerProfile applyTo(ServerProfile s) {
        return new ServerProfile(s.host(), protocols, cipherSuites, true, false, true, s.certificate(), s.http());
    }

    /** A fragment of an nginx configuration with the same settings. */
    public String nginx() {
        StringBuilder b = new StringBuilder();
        b.append("ssl_protocols ").append(String.join(" ", protocols.stream().sorted().toList())).append(";\n");
        b.append("ssl_ciphers ").append(String.join(":", cipherSuites.stream().filter(c -> !CipherSuites.tls13(c)).map(Policy::openssl).toList())).append(";\n");
        b.append("ssl_prefer_server_ciphers on;\nssl_session_tickets off;\n");
        b.append("add_header Strict-Transport-Security \"max-age=63072000; includeSubDomains\" always;\n");
        return b.toString();
    }

    private static String openssl(String iana) {
        return iana.replace("TLS_", "").replace("_WITH_", "-").replace("AES_128", "AES128").replace("AES_256", "AES256")
                .replace("CHACHA20_POLY1305_SHA256", "CHACHA20-POLY1305").replace("_", "-");
    }
}
