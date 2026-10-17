package com.jk.explore.security.backups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** A backup is a copy of every secret in the system, so the archive is sealed with AES-256-GCM and its key lives apart from it. */
final class BackupVault {

    static final class WrongKeyException extends Exception {
        WrongKeyException() {
            super("the archive did not open with this key");
        }
    }

    private BackupVault() {
    }

    static byte[] newKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    /** A key file readable only by its owner. */
    static void writeKeyFile(Path file, byte[] key) throws IOException {
        Files.write(file, key);
        Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }

    static String permissions(Path file) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    static byte[] pack(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(files.size());
            for (Map.Entry<String, byte[]> entry : new TreeMap<>(files).entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeInt(entry.getValue().length);
                data.write(entry.getValue());
            }
        }
        return out.toByteArray();
    }

    static Map<String, byte[]> unpack(byte[] archive) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(archive))) {
            int count = data.readInt();
            for (int i = 0; i < count; i++) {
                String name = data.readUTF();
                byte[] content = new byte[data.readInt()];
                data.readFully(content);
                files.put(name, content);
            }
        }
        return files;
    }

    static byte[] seal(byte[] key, Map<String, byte[]> files) throws IOException, GeneralSecurityException {
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        byte[] sealed = cipher.doFinal(pack(files));
        byte[] out = Arrays.copyOf(nonce, 12 + sealed.length);
        System.arraycopy(sealed, 0, out, 12, sealed.length);
        return out;
    }

    static Map<String, byte[]> open(byte[] key, byte[] archive) throws IOException, GeneralSecurityException, WrongKeyException {
        if (archive.length < 12 + 16) {
            throw new WrongKeyException();
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOf(archive, 12)));
        try {
            return unpack(cipher.doFinal(archive, 12, archive.length - 12));
        } catch (AEADBadTagException e) {
            throw new WrongKeyException();
        }
    }
}
