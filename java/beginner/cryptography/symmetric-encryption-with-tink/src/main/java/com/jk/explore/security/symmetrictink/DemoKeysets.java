package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Keysets with fixed contents, so the demo prints the same text on every run. A real keyset is made with
 * {@code KeysetHandle.generateNew}, which picks a random key and a random key id, and is never written in the clear.
 * The key bytes here are public and must never protect real data.
 */
final class DemoKeysets {

    private DemoKeysets() {
    }

    /** A keyset holding one AES-256-GCM key with the given id, whose 32 key bytes are the text repeated or cut to size. */
    static KeysetHandle aes256Gcm(int keyId, String keyText) {
        byte[] keyBytes = new byte[32];
        byte[] text = keyText.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = text[i % text.length];
        }
        // The key as Tink stores it: a protocol buffer message with the key bytes in field 3.
        byte[] proto = new byte[2 + keyBytes.length];
        proto[0] = 0x1a;
        proto[1] = (byte) keyBytes.length;
        System.arraycopy(keyBytes, 0, proto, 2, keyBytes.length);
        String json = "{\"primaryKeyId\":" + keyId + ",\"key\":[{\"keyData\":{\"typeUrl\":"
                + "\"type.googleapis.com/google.crypto.tink.AesGcmKey\",\"value\":\""
                + Base64.getEncoder().encodeToString(proto) + "\",\"keyMaterialType\":\"SYMMETRIC\"},"
                + "\"status\":\"ENABLED\",\"keyId\":" + keyId + ",\"outputPrefixType\":\"TINK\"}]}";
        try {
            return CleartextKeysetHandle.read(JsonKeysetReader.withString(json));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The raw 32 key bytes, to check that they do not appear in a keyset that was stored encrypted. */
    static byte[] keyBytes(String keyText) {
        byte[] keyBytes = new byte[32];
        byte[] text = keyText.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = text[i % text.length];
        }
        return keyBytes;
    }
}
