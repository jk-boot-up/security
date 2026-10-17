package com.jk.explore.security.keystore;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Messages sealed with AES-256-GCM under a key from the vault. Each message starts with the name of the key that sealed it, so after a
 * key is replaced the old messages can still be opened by looking up the old key, and new messages use the new one.
 *
 * <p>Layout: one byte for the length of the key name, the key name, a 12-byte nonce, then the cipher text with its tag.
 */
public final class Messages {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Messages() {
    }

    public static byte[] seal(KeystoreVault vault, String keyName, byte[] plaintext) {
        byte[] name = keyName.getBytes(StandardCharsets.UTF_8);
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, vault.get(keyName), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(name);
            byte[] sealed = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(1 + name.length + nonce.length + sealed.length)
                    .put((byte) name.length).put(name).put(nonce).put(sealed).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The name of the key a message was sealed with. */
    public static String keyNameOf(byte[] message) {
        int length = message[0] & 0xFF;
        return new String(message, 1, length, StandardCharsets.UTF_8);
    }

    /** Opens a message with the key named in it, or throws {@link CannotOpenException} if that key is gone or the message was changed. */
    public static byte[] open(KeystoreVault vault, byte[] message) {
        String keyName = keyNameOf(message);
        SecretKey key = vault.has(keyName) ? vault.get(keyName) : null;
        if (key == null) {
            throw new CannotOpenException("the key " + keyName + " is not in the vault");
        }
        int offset = 1 + keyName.length();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, message, offset, 12));
            cipher.updateAAD(keyName.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(message, offset + 12, message.length - offset - 12);
        } catch (AEADBadTagException e) {
            throw new CannotOpenException("the message was changed, or the key is not the one that sealed it");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Thrown when a message cannot be opened. */
    public static class CannotOpenException extends RuntimeException {
        public CannotOpenException(String message) {
            super(message);
        }
    }
}
