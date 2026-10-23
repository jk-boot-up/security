package com.jk.explore.security.hsmkeys;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.function.UnaryOperator;

/** A few lines of DER, enough to build a self-signed certificate whose signature is made by someone else (the token). */
final class Der {

    private Der() {
    }

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            body.writeBytes(p);
        }
        byte[] b = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (b.length < 128) {
            out.write(b.length);
        } else if (b.length < 256) {
            out.write(0x81);
            out.write(b.length);
        } else {
            out.write(0x82);
            out.write(b.length >> 8);
            out.write(b.length & 0xFF);
        }
        out.writeBytes(b);
        return out.toByteArray();
    }

    static byte[] seq(byte[]... parts) {
        return tlv(0x30, parts);
    }

    static byte[] integer(BigInteger v) {
        return tlv(0x02, v.toByteArray());
    }

    static byte[] oid(int... arcs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(arcs[0] * 40 + arcs[1]);
        for (int i = 2; i < arcs.length; i++) {
            int v = arcs[i];
            int n = v >= 1 << 14 ? 2 : v >= 1 << 7 ? 1 : 0;
            for (int k = n; k >= 0; k--) {
                out.write(((v >> (7 * k)) & 0x7F) | (k > 0 ? 0x80 : 0));
            }
        }
        return tlv(0x06, out.toByteArray());
    }

    static byte[] name(String org, String cn) {
        return seq(rdn(new int[] {2, 5, 4, 10}, org), rdn(new int[] {2, 5, 4, 3}, cn));
    }

    private static byte[] rdn(int[] oid, String value) {
        return tlv(0x31, seq(oid(oid), tlv(0x0C, value.getBytes(StandardCharsets.UTF_8))));
    }

    /** A self-signed X.509 v3 certificate with SHA256withRSA. The signer gets the to-be-signed bytes and returns the RSA signature. */
    static byte[] selfSignedRsa(String org, String cn, PublicKey key, BigInteger serial, String notBefore, String notAfter, UnaryOperator<byte[]> signer) {
        byte[] algorithm = seq(oid(1, 2, 840, 113549, 1, 1, 11), tlv(0x05));
        byte[] name = name(org, cn);
        byte[] tbs = seq(tlv(0xA0, integer(BigInteger.TWO)), integer(serial), algorithm, name,
                seq(tlv(0x17, notBefore.getBytes(StandardCharsets.US_ASCII)), tlv(0x17, notAfter.getBytes(StandardCharsets.US_ASCII))), name, key.getEncoded());
        byte[] sig = signer.apply(tbs);
        byte[] bits = new byte[sig.length + 1];
        System.arraycopy(sig, 0, bits, 1, sig.length);
        return seq(tbs, algorithm, tlv(0x03, bits));
    }
}
