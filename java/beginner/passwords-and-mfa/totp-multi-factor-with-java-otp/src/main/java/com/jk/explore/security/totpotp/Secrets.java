package com.jk.explore.security.totpotp;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * The keys of the published test vectors of RFC 4226 and RFC 6238, one for each hash. They are public, so the demo's
 * codes can be checked against the standards, and they must never protect a real account.
 */
final class Secrets {

    static final byte[] SHA1 = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    static final byte[] SHA256 = "12345678901234567890123456789012".getBytes(StandardCharsets.US_ASCII);
    static final byte[] SHA512 =
            "1234567890123456789012345678901234567890123456789012345678901234".getBytes(StandardCharsets.US_ASCII);

    private Secrets() {
    }

    static SecretKey key(byte[] bytes, String algorithm) {
        return new SecretKeySpec(bytes, algorithm);
    }
}
