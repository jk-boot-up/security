package com.jk.explore.security.symmetric;

/** Where the 12-byte nonces for AES-GCM come from. A nonce is a number used once: with a given key, it must never repeat. */
public interface NonceSource {
    byte[] next();
}
