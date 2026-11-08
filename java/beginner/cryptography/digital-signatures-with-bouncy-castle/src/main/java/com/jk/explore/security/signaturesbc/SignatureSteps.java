package com.jk.explore.security.signaturesbc;

import java.math.BigInteger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;

/**
 * An ECDSA signature on P-256, made and checked step by step with plain arithmetic, so each value can be printed.
 * A test shows that it gives exactly Bouncy Castle's deterministic signature, and the r and s published in RFC 6979.
 */
final class SignatureSteps {

    private SignatureSteps() {
    }

    /** The values of the four signing steps: the hash h, the one-time number k, then the signature r and s. */
    record Steps(BigInteger h, BigInteger k, BigInteger r, BigInteger s) {
    }

    static Steps sign(BigInteger key, byte[] message) {
        BigInteger n = Ecdsa.P256.getN();                                   // the number of points on the curve
        BigInteger h = new BigInteger(1, Ecdsa.sha256(message));            // step 1: hash the message
        HMacDSAKCalculator nonces = new HMacDSAKCalculator(new SHA256Digest());
        nonces.init(n, key, Ecdsa.sha256(message));
        BigInteger k = nonces.nextK();                                      // step 2: k from the key and h
        BigInteger r = x(Ecdsa.P256.getG().multiply(k)).mod(n);             // step 3: r = x of k times G
        BigInteger s = k.modInverse(n).multiply(h.add(r.multiply(key))).mod(n);   // step 4: s = (h + r key) / k
        return new Steps(h, k, r, s);
    }

    /** The check, with the public key Q only: the x of (h / s) G + (r / s) Q must equal r. Returns that x. */
    static BigInteger check(ECPoint publicKey, byte[] message, BigInteger r, BigInteger s) {
        BigInteger n = Ecdsa.P256.getN();
        BigInteger h = new BigInteger(1, Ecdsa.sha256(message));
        BigInteger w = s.modInverse(n);
        ECPoint point = Ecdsa.P256.getG().multiply(h.multiply(w).mod(n)).add(publicKey.multiply(r.multiply(w).mod(n)));
        return x(point).mod(n);
    }

    private static BigInteger x(ECPoint point) {
        return point.normalize().getAffineXCoord().toBigInteger();
    }
}
