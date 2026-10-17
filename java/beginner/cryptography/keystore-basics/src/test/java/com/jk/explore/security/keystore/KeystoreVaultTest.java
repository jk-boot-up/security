package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeystoreVaultTest {

    private static final byte[] KEY = Hex.bytes("keystore-statement-key-32-bytes!");

    @TempDir
    Path dir;

    @Test
    void aKeySavedAndReopenedComesBack() {
        Path file = dir.resolve("a.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("statements-v1", KEY).save(file);
        assertArrayEquals(KEY, KeystoreVault.open(file, KeystoreDemo.PASSWORD).get("statements-v1").getEncoded());
    }

    @Test
    void theFileShowsNeitherTheKeyNorItsName() throws IOException {
        Path file = dir.resolve("b.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("statements-v1", KEY).save(file);
        byte[] bytes = Files.readAllBytes(file);
        assertFalse(Hex.contains(bytes, KEY));
        assertFalse(Hex.contains(bytes, Hex.bytes("statements-v1")));
    }

    @Test
    void aWrongPasswordIsRefused() {
        Path file = dir.resolve("c.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("k", KEY).save(file);
        assertThrows(KeystoreVault.WrongPasswordException.class, () -> KeystoreVault.open(file, "wrong".toCharArray()));
    }

    @Test
    void aChangedFileIsRefused() throws IOException {
        Path file = dir.resolve("d.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("k", KEY).save(file);
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 10] ^= 1;
        Files.write(file, bytes);
        assertThrows(KeystoreVault.WrongPasswordException.class, () -> KeystoreVault.open(file, KeystoreDemo.PASSWORD));
    }

    @Test
    void aMissingAliasGivesNothing() {
        assertNull(KeystoreVault.create(KeystoreDemo.PASSWORD).get("nope"));
    }

    @Test
    void aliasesCanBeListedAndRemoved() {
        KeystoreVault vault = KeystoreVault.create(KeystoreDemo.PASSWORD).put("a", KEY).put("b", KEY);
        assertEquals(List.of("a", "b"), vault.aliases().stream().sorted().toList());
        vault.remove("a");
        assertFalse(vault.has("a"));
        assertTrue(vault.has("b"));
    }

    @Test
    void theDefaultFileRecordsTenThousandRoundsAndARaisedSettingShowsUpInOnePlace() throws IOException {
        Path normal = dir.resolve("e.p12");
        Path raised = dir.resolve("f.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("k", KEY).save(normal);
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("k", KEY).saveWithRounds(raised, 600_000);
        assertEquals(List.of(10_000), KeystoreVault.allRoundsRecordedIn(Files.readAllBytes(normal)));
        assertTrue(KeystoreVault.allRoundsRecordedIn(Files.readAllBytes(raised)).contains(600_000));
    }

    @Test
    void aFileSavedWithRaisedRoundsStillOpensWithThePassword() {
        Path file = dir.resolve("g.p12");
        KeystoreVault.create(KeystoreDemo.PASSWORD).put("k", KEY).saveWithRounds(file, 600_000);
        assertArrayEquals(KEY, KeystoreVault.open(file, KeystoreDemo.PASSWORD).get("k").getEncoded());
    }
}
