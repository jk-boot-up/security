package com.jk.explore.security.tlslog;

/** The two application traffic secrets of a connection, one for each direction. These are what a key log file holds. */
record Secrets(byte[] client, byte[] server) {

    /**
     * The TLS 1.3 key schedule from the shared secret to the application traffic secrets. The transcript is simplified to the hello values; the shape is the real one:
     * early secret, handshake secret (mixing in the shared secret), master secret, then one traffic secret for each direction.
     */
    static Secrets derive(byte[] sharedSecret, byte[] transcriptHash) {
        byte[] emptyHash = Hkdf.sha256();
        byte[] early = Hkdf.extract(Hkdf.ZEROS, Hkdf.ZEROS);
        byte[] handshake = Hkdf.extract(Hkdf.deriveSecret(early, "derived", emptyHash), sharedSecret);
        byte[] master = Hkdf.extract(Hkdf.deriveSecret(handshake, "derived", emptyHash), Hkdf.ZEROS);
        return new Secrets(Hkdf.deriveSecret(master, "c ap traffic", transcriptHash), Hkdf.deriveSecret(master, "s ap traffic", transcriptHash));
    }
}
