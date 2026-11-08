package com.jk.explore.security.symmetric;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Works out a GCM tag by hand, one step at a time, so the steps can be printed. It is for teaching only: real code lets
 * the JDK's AES/GCM make the tag, and the tests check that this gives exactly the same tag.
 *
 * <ol>
 *   <li>The hash key H is AES(key, sixteen zero bytes).</li>
 *   <li>Every 16-byte block of the associated data, then of the cipher text, then one block holding both lengths, is
 *       stirred into a running value X: X = (X xor block) times H. The "times" is GCM's own multiplication of 128-bit
 *       values (in the field GF(2^128)): every bit of the result depends on every bit of X and H.</li>
 *   <li>The mask is AES(key, nonce followed by 00000001). The cipher text itself was made from 00000002 onward.</li>
 *   <li>The tag is X xor the mask.</li>
 * </ol>
 */
public final class TagSteps {

    private static final int BLOCK = 16;

    /** One block stirred into X: what it is, its bytes, and X after it. */
    public record Mix(String name, byte[] block, byte[] after) {
    }

    /** Every value along the way. */
    public record Steps(byte[] hashKey, List<Mix> mixes, byte[] counterBlock, byte[] mask, byte[] tag) {
    }

    private TagSteps() {
    }

    public static Steps of(byte[] key, byte[] nonce, byte[] associatedData, byte[] ciphertext) {
        byte[] h = EcbCipher.encrypt(key, new byte[BLOCK]);
        List<Mix> mixes = new ArrayList<>();
        byte[] x = new byte[BLOCK];
        x = stir(mixes, x, h, "account", associatedData);
        x = stir(mixes, x, h, "cipher text", ciphertext);
        byte[] lengths = ByteBuffer.allocate(BLOCK).putLong(associatedData.length * 8L).putLong(ciphertext.length * 8L).array();
        x = multiply(Hex.xor(x, lengths, BLOCK), h);
        mixes.add(new Mix("lengths", lengths, x));
        byte[] counter = Arrays.copyOf(nonce, BLOCK);
        counter[BLOCK - 1] = 1;
        byte[] mask = EcbCipher.encrypt(key, counter);
        return new Steps(h, mixes, counter, mask, Hex.xor(x, mask, BLOCK));
    }

    private static byte[] stir(List<Mix> mixes, byte[] x, byte[] h, String name, byte[] data) {
        for (int i = 0; i < data.length; i += BLOCK) {
            byte[] block = Arrays.copyOf(Arrays.copyOfRange(data, i, Math.min(i + BLOCK, data.length)), BLOCK);  // zero padded
            x = multiply(Hex.xor(x, block, BLOCK), h);
            mixes.add(new Mix(name, block, x));
        }
        return x;
    }

    /** GCM's multiplication of two 128-bit values (NIST SP 800-38D, algorithm 1). */
    static byte[] multiply(byte[] x, byte[] y) {
        byte[] z = new byte[BLOCK];
        byte[] v = y.clone();
        for (int i = 0; i < 128; i++) {
            if ((x[i / 8] >> (7 - i % 8) & 1) == 1) {
                z = Hex.xor(z, v, BLOCK);
            }
            boolean low = (v[BLOCK - 1] & 1) == 1;
            for (int j = BLOCK - 1; j > 0; j--) {
                v[j] = (byte) ((v[j] & 0xff) >>> 1 | (v[j - 1] & 1) << 7);
            }
            v[0] = (byte) ((v[0] & 0xff) >>> 1);
            if (low) {
                v[0] ^= (byte) 0xe1;
            }
        }
        return z;
    }
}
