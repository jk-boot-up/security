package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.aead.AesGcmKey;
import com.google.crypto.tink.aead.AesGcmParameters;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.util.SecretBytes;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Envelope encryption: each message gets its own fresh 256-bit data key and is sealed with it using Tink's AES-GCM; the data
 * key is then wrapped by a master {@link Aead}, which in a real system lives in a key management service or a hardware module,
 * and the wrapped key travels with the message. The master key is asked to handle only a 32-byte key, however long the message is.
 *
 * <p>Stored layout: a 4-byte length, the wrapped data key, then the cipher text.
 */
public class EnvelopeAead implements Aead {

    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try {
            AeadConfig.register();
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Aead master;

    public EnvelopeAead(Aead master) {
        this.master = master;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
        byte[] dataKey = new byte[32];
        RANDOM.nextBytes(dataKey);
        byte[] wrapped = master.encrypt(dataKey, associatedData);
        byte[] cipherText = aeadFor(dataKey).encrypt(plaintext, associatedData);
        return ByteBuffer.allocate(4 + wrapped.length + cipherText.length).putInt(wrapped.length).put(wrapped).put(cipherText).array();
    }

    @Override
    public byte[] decrypt(byte[] message, byte[] associatedData) throws GeneralSecurityException {
        if (message.length < 4) {
            throw new GeneralSecurityException("message too short");
        }
        int length = ByteBuffer.wrap(message).getInt();
        if (length < 0 || 4 + length > message.length) {
            throw new GeneralSecurityException("bad wrapped key length");
        }
        byte[] dataKey = master.decrypt(Arrays.copyOfRange(message, 4, 4 + length), associatedData);
        return aeadFor(dataKey).decrypt(Arrays.copyOfRange(message, 4 + length, message.length), associatedData);
    }

    /** A Tink AES-256-GCM primitive built from raw key bytes, with no prefix so the message layout stays simple. */
    private static Aead aeadFor(byte[] keyBytes) throws GeneralSecurityException {
        AesGcmParameters parameters = AesGcmParameters.builder().setKeySizeBytes(32).setIvSizeBytes(12).setTagSizeBytes(16)
                .setVariant(AesGcmParameters.Variant.NO_PREFIX).build();
        AesGcmKey key = AesGcmKey.builder().setParameters(parameters)
                .setKeyBytes(SecretBytes.copyFrom(keyBytes, InsecureSecretKeyAccess.get())).build();
        KeysetHandle handle = KeysetHandle.newBuilder().addEntry(KeysetHandle.importKey(key).withRandomId().makePrimary()).build();
        return handle.getPrimitive(RegistryConfiguration.get(), Aead.class);
    }
}
