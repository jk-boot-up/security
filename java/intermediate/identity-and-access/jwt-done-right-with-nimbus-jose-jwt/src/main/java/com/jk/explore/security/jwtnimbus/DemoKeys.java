package com.jk.explore.security.jwtnimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Fixed P-256 keys, so the demo prints the same key set on every run. They come from fixed seeds and protect nothing. Never do
 * this outside a demo.
 */
final class DemoKeys {

    private DemoKeys() {
    }

    /** A P-256 key made from a fixed text, the same on every run. */
    static ECKey fromSeed(String kid, String seed) throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyID(kid)
                .secureRandom(new FixedRandom(seed))
                .generate();
    }

    /** Hands out SHA-256(seed, counter) blocks instead of random bytes. */
    private static final class FixedRandom extends SecureRandom {

        private final byte[] seed;
        private int counter;

        FixedRandom(String seed) {
            this.seed = seed.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void nextBytes(byte[] bytes) {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                for (int i = 0; i < bytes.length; i += 32) {
                    sha.update(seed);
                    sha.update((byte) counter++);
                    byte[] block = sha.digest();
                    System.arraycopy(block, 0, bytes, i, Math.min(32, bytes.length - i));
                }
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
