package com.jk.explore.security.signaturesbc;

import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.SecureRandom;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSAKCalculator;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.crypto.signers.StandardDSAEncoding;
import org.bouncycastle.math.ec.ECPoint;

/**
 * ECDSA on P-256 with the nonce chosen two ways. ECDSA needs a fresh secret number k for every signature. Randomized signing
 * draws k from a random source, so every signature differs and a weak random source leaks the key. Deterministic signing
 * (RFC 6979) derives k from the key and the message with HMAC, so the same message gives the same signature and no random source is needed.
 */
final class Ecdsa {

    static final ECDomainParameters P256;

    static {
        X9ECParameters curve = NISTNamedCurves.getByName("P-256");
        P256 = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
    }

    private Ecdsa() {
    }

    record Signature(BigInteger r, BigInteger s) {
        byte[] der() {
            try {
                return StandardDSAEncoding.INSTANCE.encode(P256.getN(), r, s);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static byte[] sha256(byte[] message) {
        SHA256Digest digest = new SHA256Digest();
        digest.update(message, 0, message.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    static BigInteger privateValue(java.security.PrivateKey key) {
        return ((ECPrivateKey) key).getS();
    }

    static ECPublicKeyParameters publicFor(BigInteger d) {
        ECPoint q = P256.getG().multiply(d).normalize();
        return new ECPublicKeyParameters(q, P256);
    }

    /** Deterministic (RFC 6979, HMAC-SHA-256). */
    static Signature signDeterministic(BigInteger d, byte[] message) {
        return sign(new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest())), d, message);
    }

    /** Randomized, with k drawn from a secure random source. */
    static Signature signRandomized(BigInteger d, byte[] message) {
        return sign(new ECDSASigner(), d, message);
    }

    /** Signs with a nonce the caller supplies. This is the mistake: a k that is reused, or guessable, gives the private key away. */
    static Signature signWithNonce(BigInteger d, byte[] message, BigInteger k) {
        return sign(new ECDSASigner(new FixedNonce(k)), d, message);
    }

    private static Signature sign(ECDSASigner signer, BigInteger d, byte[] message) {
        signer.init(true, new org.bouncycastle.crypto.params.ParametersWithRandom(new ECPrivateKeyParameters(d, P256), new SecureRandom()));
        BigInteger[] rs = signer.generateSignature(sha256(message));
        return new Signature(rs[0], rs[1]);
    }

    static boolean verify(ECPublicKeyParameters key, byte[] message, Signature signature) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(false, key);
        return signer.verifySignature(sha256(message), signature.r(), signature.s());
    }

    static boolean verifyDer(ECPublicKeyParameters key, byte[] message, byte[] der) {
        try {
            BigInteger[] rs = StandardDSAEncoding.INSTANCE.decode(P256.getN(), der);
            return verify(key, message, new Signature(rs[0], rs[1]));
        } catch (java.io.IOException | RuntimeException e) {
            return false;
        }
    }

    /** What two signatures made with the same nonce reveal: k = (h1 - h2) / (s1 - s2), then d = (s1 k - h1) / r, all modulo the curve order. */
    static BigInteger recoverKeyFromReusedNonce(byte[] message1, Signature s1, byte[] message2, Signature s2) {
        BigInteger n = P256.getN();
        BigInteger h1 = new BigInteger(1, sha256(message1));
        BigInteger h2 = new BigInteger(1, sha256(message2));
        BigInteger k = h1.subtract(h2).multiply(s1.s().subtract(s2.s()).modInverse(n)).mod(n);
        return s1.s().multiply(k).subtract(h1).multiply(s1.r().modInverse(n)).mod(n);
    }

    private static final class FixedNonce implements DSAKCalculator {
        private final BigInteger k;

        FixedNonce(BigInteger k) {
            this.k = k;
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }

        @Override
        public void init(BigInteger n, SecureRandom random) {
            // the nonce is fixed on purpose
        }

        @Override
        public void init(BigInteger n, BigInteger d, byte[] message) {
            // the nonce is fixed on purpose
        }

        @Override
        public BigInteger nextK() {
            return k;
        }
    }
}
