package com.jk.explore.security.asymmetric;

import java.math.BigInteger;

/**
 * RSA and Diffie-Hellman key agreement worked out by hand, with numbers small enough to follow. The maths is the same one
 * Java's RSA and DH use (the tests prove it on real keys); only the size differs. Never use numbers this small.
 */
final class ByHand {

    private ByHand() {
    }

    /** A tiny RSA key pair: the public key is (n, e), the private key is d. */
    record RsaKeys(long p, long q, long n, long e, long d) {
    }

    /** Makes a key pair from two primes: n = p x q, and d is chosen so that e x d = 1 more than a multiple of (p-1)(q-1). */
    static RsaKeys rsaKeys(long p, long q, long e) {
        long n = p * q;
        long phi = (p - 1) * (q - 1);
        long d = BigInteger.valueOf(e).modInverse(BigInteger.valueOf(phi)).longValue();
        return new RsaKeys(p, q, n, e, d);
    }

    /** Locks with the public key: c = m^e mod n. */
    static long lock(RsaKeys keys, long m) {
        return power(m, keys.e(), keys.n());
    }

    /** Opens with the private key: m = c^d mod n. */
    static long open(RsaKeys keys, long c) {
        return power(c, keys.d(), keys.n());
    }

    /** base^exponent mod modulus: the one operation behind both RSA and Diffie-Hellman. */
    static long power(long base, long exponent, long modulus) {
        return power(BigInteger.valueOf(base), BigInteger.valueOf(exponent), BigInteger.valueOf(modulus)).longValue();
    }

    static BigInteger power(BigInteger base, BigInteger exponent, BigInteger modulus) {
        return base.modPow(exponent, modulus);
    }
}
