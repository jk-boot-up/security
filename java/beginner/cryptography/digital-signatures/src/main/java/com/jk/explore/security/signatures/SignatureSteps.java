package com.jk.explore.security.signatures;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Ed25519 redone by hand, for teaching only, following RFC 8032 (section 5.1): the five steps that Java's
 * {@code Signature.getInstance("Ed25519")} hides. A test proves that it gives Java's exact signature.
 *
 * <p>"x B" means "times the base point B": a point on the curve added to itself that many times. It is quick to work out
 * and, as far as anyone knows, impossible to undo, which is why the public key A = a x B does not give away the secret a.
 * "mod L" keeps the remainder after dividing by L, the number of points that B reaches.
 *
 * <p>Not constant-time and not checked against bad input: never use it for anything real.
 */
final class SignatureSteps {

    static final BigInteger P = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19));
    static final BigInteger L = BigInteger.TWO.pow(252).add(new BigInteger("27742317777372353535851937790883648493"));
    private static final BigInteger D = BigInteger.valueOf(-121665).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P);
    private static final BigInteger SQRT_MINUS_1 = BigInteger.TWO.modPow(P.subtract(BigInteger.ONE).shiftRight(2), P);
    private static final BigInteger[] B = decode(littleEndian(BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)));

    /** Every value the five steps make, as 32 bytes each (numbers in little-endian order, as Ed25519 writes them). */
    record Steps(byte[] secretA, byte[] prefix, byte[] publicA, byte[] r, byte[] bigR, byte[] k, byte[] s) {
        byte[] signature() {
            byte[] out = Arrays.copyOf(bigR, 64);
            System.arraycopy(s, 0, out, 32, 32);
            return out;
        }
    }

    private SignatureSteps() {
    }

    /**
     * Step 1: hash the private key; the first half, with 3 low bits and the top bit fixed, is the secret a, the second half a
     * prefix, and the public key is A = a x B. Step 2: r from the prefix and the message, the same for the same message.
     * Step 3: R = r x B. Step 4: k ties R, A and the message. Step 5: S = r + k x a. The signature is R then S.
     */
    static Steps sign(byte[] privateSeed, byte[] message) {
        byte[] h = sha512(privateSeed);
        BigInteger a = number(Arrays.copyOfRange(h, 0, 32)).andNot(BigInteger.valueOf(7)).clearBit(255).setBit(254);
        byte[] prefix = Arrays.copyOfRange(h, 32, 64);
        byte[] publicA = encode(times(a, B));
        BigInteger r = number(sha512(prefix, message)).mod(L);
        byte[] bigR = encode(times(r, B));
        BigInteger k = number(sha512(bigR, publicA, message)).mod(L);
        BigInteger s = r.add(k.multiply(a)).mod(L);
        return new Steps(littleEndian(a), prefix, publicA, littleEndian(r), bigR, littleEndian(k), littleEndian(s));
    }

    /** The check with the public key only: S x B must equal R + k x A. */
    static boolean checks(byte[] publicA, byte[] message, byte[] signature) {
        byte[] bigR = Arrays.copyOfRange(signature, 0, 32);
        BigInteger s = number(Arrays.copyOfRange(signature, 32, 64));
        BigInteger k = number(sha512(bigR, publicA, message)).mod(L);
        return Arrays.equals(encode(times(s, B)), encode(add(decode(bigR), times(k, decode(publicA)))));
    }

    // ---- the curve: points as (X, Y, Z, T) with x = X/Z, y = Y/Z, as in RFC 8032

    private static BigInteger[] add(BigInteger[] p, BigInteger[] q) {
        BigInteger a = p[1].subtract(p[0]).multiply(q[1].subtract(q[0])).mod(P);
        BigInteger b = p[1].add(p[0]).multiply(q[1].add(q[0])).mod(P);
        BigInteger c = p[3].multiply(q[3]).multiply(D).shiftLeft(1).mod(P);
        BigInteger d = p[2].multiply(q[2]).shiftLeft(1).mod(P);
        BigInteger e = b.subtract(a), f = d.subtract(c), g = d.add(c), hh = b.add(a);
        return new BigInteger[] {e.multiply(f).mod(P), g.multiply(hh).mod(P), f.multiply(g).mod(P), e.multiply(hh).mod(P)};
    }

    private static BigInteger[] times(BigInteger n, BigInteger[] point) {
        BigInteger[] sum = {BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};   // the neutral point
        for (int i = n.bitLength() - 1; i >= 0; i--) {                                           // double, and add
            sum = add(sum, sum);
            if (n.testBit(i)) {
                sum = add(sum, point);
            }
        }
        return sum;
    }

    /** A point as 32 bytes: y, with the lowest bit of x in the top bit. */
    private static byte[] encode(BigInteger[] point) {
        BigInteger z = point[2].modInverse(P);
        BigInteger x = point[0].multiply(z).mod(P);
        BigInteger y = point[1].multiply(z).mod(P);
        return littleEndian(x.testBit(0) ? y.setBit(255) : y);
    }

    private static BigInteger[] decode(byte[] bytes) {
        BigInteger y = number(bytes).clearBit(255);
        boolean xOdd = (bytes[31] & 0x80) != 0;
        BigInteger yy = y.multiply(y);
        BigInteger xx = yy.subtract(BigInteger.ONE).multiply(D.multiply(yy).add(BigInteger.ONE).modInverse(P)).mod(P);
        BigInteger x = xx.modPow(P.add(BigInteger.valueOf(3)).shiftRight(3), P);
        if (!x.multiply(x).subtract(xx).mod(P).equals(BigInteger.ZERO)) {
            x = x.multiply(SQRT_MINUS_1).mod(P);
        }
        if (x.testBit(0) != xOdd) {
            x = P.subtract(x);
        }
        return new BigInteger[] {x, y, BigInteger.ONE, x.multiply(y).mod(P)};
    }

    // ---- bytes and numbers

    private static BigInteger number(byte[] littleEndian) {
        byte[] big = new byte[littleEndian.length];
        for (int i = 0; i < big.length; i++) {
            big[i] = littleEndian[littleEndian.length - 1 - i];
        }
        return new BigInteger(1, big);
    }

    private static byte[] littleEndian(BigInteger n) {
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            out[i] = n.shiftRight(8 * i).byteValue();
        }
        return out;
    }

    private static byte[] sha512(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            for (byte[] part : parts) {
                digest.update(part);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
