package com.jk.explore.security.symmetric;

import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES in ECB mode: every 16-byte block is encrypted on its own, with nothing to tell one block from the next. The same
 * block of plain text always gives the same block of cipher text, so patterns in the data show through. It is here to
 * show what not to do.
 */
public final class EcbCipher {

    public static final int BLOCK = 16;

    private EcbCipher() {
    }

    /** Encrypts data whose length is a multiple of 16. */
    public static byte[] encrypt(byte[] key, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** How many different 16-byte blocks there are in the data. */
    public static int distinctBlocks(byte[] data) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < data.length; i += BLOCK) {
            seen.add(Hex.of(java.util.Arrays.copyOfRange(data, i, i + BLOCK)));
        }
        return seen.size();
    }
}
