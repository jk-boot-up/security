package com.jk.explore.security.k8ssecrets;

import java.security.SecureRandom;
import java.util.Base64;

/** The EncryptionConfiguration file the API server reads, and the marker that an encrypted value carries in etcd. */
final class Encryption {

    /** What etcd holds in front of a value the API server has encrypted with aescbc and the key named key1. */
    static final String MARKER = "k8s:enc:aescbc:v1:key1";

    private Encryption() {
    }

    /** A new random 256-bit key, as base64 (the format the file wants). Never printed. */
    static String newKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /** Secrets are stored as they are: this is what a cluster has before anyone configures anything. */
    static String identityOnly() {
        return """
                apiVersion: apiserver.config.k8s.io/v1
                kind: EncryptionConfiguration
                resources:
                  - resources: [secrets]
                    providers:
                      - identity: {}
                """;
    }

    /** New writes are encrypted with aescbc; identity stays last so values written earlier can still be read. */
    static String aescbc(String keyBase64) {
        return """
                apiVersion: apiserver.config.k8s.io/v1
                kind: EncryptionConfiguration
                resources:
                  - resources: [secrets]
                    providers:
                      - aescbc:
                          keys:
                            - name: key1
                              secret: %s
                      - identity: {}
                """.formatted(keyBase64);
    }
}
