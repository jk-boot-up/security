package com.jk.explore.security.symmetrictink;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A Tink AES-GCM message built by hand with the JDK, to show its layout: a version byte, the 4-byte key id, the 12-byte
 * nonce, the cipher text and the 16-byte tag. Tink itself picks a random nonce; here the caller passes one, so the demo
 * can print the same bytes on every run, and a test shows Tink opens the hand-built message.
 */
public final class TinkLayout {

    public static final int PREFIX = 5;
    public static final int NONCE = 12;
    public static final int TAG = 16;

    /** The parts of one message, in the order they are stored. */
    public record Parts(byte[] prefix, byte[] nonce, byte[] cipherText, byte[] tag) {
    }

    private TinkLayout() {
    }

    /** The first five bytes: version 1, then the key id as a 4-byte number (1001 is 000003e9). */
    public static byte[] prefix(int keyId) {
        return ByteBuffer.allocate(PREFIX).put((byte) 1).putInt(keyId).array();
    }

    /** prefix + nonce + AES-GCM(key, nonce, associated data, plain text), the way Tink lays it out. */
    public static byte[] seal(byte[] key, int keyId, byte[] nonce, byte[] associatedData, byte[] plaintext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG * 8, nonce));
        cipher.updateAAD(associatedData);
        byte[] cipherTextAndTag = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(PREFIX + NONCE + cipherTextAndTag.length)
                .put(prefix(keyId)).put(nonce).put(cipherTextAndTag).array();
    }

    /** Cuts a message into its parts: 5 + 12 + the cipher text + 16. */
    public static Parts split(byte[] message) {
        int tagStart = message.length - TAG;
        return new Parts(Arrays.copyOfRange(message, 0, PREFIX),
                Arrays.copyOfRange(message, PREFIX, PREFIX + NONCE),
                Arrays.copyOfRange(message, PREFIX + NONCE, tagStart),
                Arrays.copyOfRange(message, tagStart, message.length));
    }
}
