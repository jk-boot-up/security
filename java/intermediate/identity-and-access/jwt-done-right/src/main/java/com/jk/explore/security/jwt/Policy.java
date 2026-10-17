package com.jk.explore.security.jwt;

/**
 * What the bank's accounts API accepts. The algorithm and the token type are fixed here, by the server: a token never gets to choose
 * them (RFC 8725 sections 3.1 and 3.11).
 */
record Policy(String issuer, String audience, String requiredScope, long skewSeconds, long maxLifetimeSeconds) {

    static final String ALGORITHM = "EdDSA";
    static final String TYPE = "at+jwt";

    /** The bank's settings: five-minute tokens, thirty seconds of clock skew, nothing longer than fifteen minutes. */
    static Policy bank() {
        return new Policy("https://auth.bank.example", "accounts-api", "accounts:read", 30, 900);
    }
}
