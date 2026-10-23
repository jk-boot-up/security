package com.jk.explore.security.cryptoagility;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A stored value with a header that says how it was protected: magic byte, suite version, key id. The header is authenticated together with the data, so it
 * cannot be changed on the way. The reader picks the suite from the header, and a policy decides which versions it will still read and which one is written.
 */
public final class Envelope {

    public static final int V0_ECB = 0;
    public static final int V1_CBC_HMAC = 1;
    public static final int V2_GCM = 2;
    public static final int V3_CHACHA = 3;
    static final byte MAGIC = 'E';

    /** Which versions may be read, and which one is written. */
    public record Rules(Set<Integer> readable, int writeVersion) {
    }

    private final KeyRing keys;
    private final SecureRandom random = new SecureRandom();

    public Envelope(KeyRing keys) {
        this.keys = keys;
    }

    public static int versionOf(byte[] blob) {
        return blob[1];
    }

    public static int keyOf(byte[] blob) {
        return blob[2];
    }

    public byte[] seal(byte[] plain, int version, int keyId) {
        byte[] header = {MAGIC, (byte) version, (byte) keyId};
        try {
            byte[] body = switch (version) {
                case V1_CBC_HMAC -> sealCbc(plain, header, keyId);
                case V2_GCM -> sealAead("AES/GCM/NoPadding", "AES", keys.derive(keyId, "gcm", 32), 12, plain, header);
                case V3_CHACHA -> sealAead("ChaCha20-Poly1305", "ChaCha20", keys.derive(keyId, "chacha", 32), 12, plain, header);
                default -> throw new IllegalArgumentException("version " + version + " is not written any more");
            };
            return ByteBuffer.allocate(3 + body.length).put(header).put(body).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] open(byte[] blob, Rules rules) throws GeneralSecurityException {
        if (blob.length < 4 || blob[0] != MAGIC) {
            throw new GeneralSecurityException("not an envelope");
        }
        int version = versionOf(blob);
        if (!rules.readable().contains(version)) {
            throw new GeneralSecurityException("version " + version + " is not accepted by the policy");
        }
        byte[] header = Arrays.copyOf(blob, 3);
        byte[] body = Arrays.copyOfRange(blob, 3, blob.length);
        int keyId = keyOf(blob);
        if (!keys.has(keyId)) {
            throw new GeneralSecurityException("key " + keyId + " is retired");
        }
        return switch (version) {
            case V1_CBC_HMAC -> openCbc(body, header, keyId);
            case V2_GCM -> openAead("AES/GCM/NoPadding", "AES", keys.derive(keyId, "gcm", 32), 12, body, header);
            case V3_CHACHA -> openAead("ChaCha20-Poly1305", "ChaCha20", keys.derive(keyId, "chacha", 32), 12, body, header);
            default -> throw new GeneralSecurityException("unknown version " + version);
        };
    }

    private byte[] sealAead(String transformation, String algorithm, byte[] key, int ivLength, byte[] plain, byte[] header) throws GeneralSecurityException {
        byte[] iv = new byte[ivLength];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, algorithm), transformation.startsWith("AES") ? new GCMParameterSpec(128, iv) : new IvParameterSpec(iv));
        cipher.updateAAD(header);
        byte[] ct = cipher.doFinal(plain);
        return ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
    }

    private byte[] openAead(String transformation, String algorithm, byte[] key, int ivLength, byte[] body, byte[] header) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, algorithm), transformation.startsWith("AES") ? new GCMParameterSpec(128, Arrays.copyOf(body, ivLength)) : new IvParameterSpec(Arrays.copyOf(body, ivLength)));
        cipher.updateAAD(header);
        return cipher.doFinal(body, ivLength, body.length - ivLength);
    }

    private byte[] sealCbc(byte[] plain, byte[] header, int keyId) throws GeneralSecurityException {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.derive(keyId, "cbc-enc", 16), "AES"), new IvParameterSpec(iv));
        byte[] ct = cipher.doFinal(plain);
        byte[] tag = mac(keyId, header, iv, ct);
        return ByteBuffer.allocate(iv.length + ct.length + tag.length).put(iv).put(ct).put(tag).array();
    }

    private byte[] openCbc(byte[] body, byte[] header, int keyId) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOf(body, 16);
        byte[] ct = Arrays.copyOfRange(body, 16, body.length - 32);
        byte[] tag = Arrays.copyOfRange(body, body.length - 32, body.length);
        if (!MessageDigest.isEqual(tag, mac(keyId, header, iv, ct))) {
            throw new GeneralSecurityException("the tag does not match");
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keys.derive(keyId, "cbc-enc", 16), "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(ct);
    }

    private byte[] mac(int keyId, byte[] header, byte[] iv, byte[] ct) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keys.derive(keyId, "cbc-mac", 32), "HmacSHA256"));
        mac.update(header);
        mac.update(iv);
        mac.update(ct);
        return mac.doFinal();
    }
}
