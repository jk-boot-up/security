package com.jk.explore.security.backups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupStoreTest {

    @TempDir
    Path dir;

    @Test
    void sha256MatchesThePublishedValueForAbc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.of("abc".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void manifestRoundTripsAsText() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Manifest manifest = Manifest.of(live);
        assertEquals(manifest, Manifest.parse(manifest.toText()));
        assertEquals(3, manifest.entries().size());
    }

    @Test
    void manifestCheckFindsMissingChangedAndExtraFiles() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Manifest manifest = Manifest.of(live);
        Files.delete(live.resolve("inventory.csv"));
        Files.writeString(live.resolve("orders.csv"), "changed");
        Files.writeString(live.resolve("new.csv"), "x");
        Manifest.Problems problems = manifest.check(live);
        assertEquals(java.util.List.of("inventory.csv"), problems.missing());
        assertEquals(java.util.List.of("orders.csv"), problems.changed());
        assertEquals(java.util.List.of("new.csv"), problems.extra());
        assertFalse(problems.ok());
    }

    @Test
    void incrementalStoresOnlyWhatChangedAndRestoreRebuildsEverything() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "d1");
        Files.writeString(live.resolve("orders.csv"), BackupsDemo.ORDERS + "1003,cy,99\n");
        BackupStore.incremental(live, root, "d2", "d1");
        assertEquals(3, BackupStore.storedFiles(root, "d1"));
        assertEquals(1, BackupStore.storedFiles(root, "d2"));
        Path restored = dir.resolve("restored");
        assertTrue(BackupStore.restore(root, "d2", restored).ok());
        assertEquals(Files.readString(live.resolve("orders.csv")), Files.readString(restored.resolve("orders.csv")));
    }

    @Test
    void restoringAnOlderSnapshotGivesTheOlderState() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "d1");
        Files.writeString(live.resolve("orders.csv"), "newer");
        BackupStore.incremental(live, root, "d2", "d1");
        Path restored = dir.resolve("restored");
        assertTrue(BackupStore.restore(root, "d1", restored).ok());
        assertEquals(BackupsDemo.ORDERS, Files.readString(restored.resolve("orders.csv")));
    }

    @Test
    void tamperingAndLossAreFoundWithoutRestoring() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "d1");
        assertTrue(BackupStore.verifyStored(root, "d1").ok());
        Files.writeString(root.resolve("d1/data/orders.csv"), "tampered");
        assertEquals(1, BackupStore.verifyStored(root, "d1").changed().size());
        Files.delete(root.resolve("d1/data/customers.csv"));
        assertEquals(1, BackupStore.verifyStored(root, "d1").missing().size());
    }

    @Test
    void aBackupThatSkippedTheOrdersFilePassesTheManifestButFailsTheRestoreTest() throws IOException {
        Path live = dir.resolve("live");
        Files.createDirectories(live);
        Files.writeString(live.resolve("customers.csv"), "id,name\nana,Ana\n");
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "partial");
        RestoreTest.Result result = RestoreTest.run(root, "partial", dir.resolve("scratch"));
        assertTrue(result.manifestOk());
        assertFalse(result.ordersUsable());
        assertFalse(result.passed());
    }

    @Test
    void aGoodBackupPassesTheRestoreTest() throws IOException {
        Path live = BackupsDemo.shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "good");
        RestoreTest.Result result = RestoreTest.run(root, "good", dir.resolve("scratch"));
        assertTrue(result.passed());
        assertEquals(2, result.orders());
    }
}
