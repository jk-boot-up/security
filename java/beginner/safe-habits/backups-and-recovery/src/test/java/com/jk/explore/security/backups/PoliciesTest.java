package com.jk.explore.security.backups;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PoliciesTest {

    @TempDir
    Path dir;

    @Test
    void threeTwoOneNeedsAllFourThings() {
        var live = new ThreeTwoOne.Copy("server", "ssd", false, false);
        var nas = new ThreeTwoOne.Copy("nas", "disk", false, false);
        var cloud = new ThreeTwoOne.Copy("cloud", "object", true, true);
        assertEquals(4, ThreeTwoOne.check(List.of(live, nas, cloud)).score());
        assertFalse(ThreeTwoOne.check(List.of(live, nas)).threeCopies());
        assertFalse(ThreeTwoOne.check(List.of(live, nas, new ThreeTwoOne.Copy("x", "disk", false, false))).oneOffsite());
        assertFalse(ThreeTwoOne.check(List.of(live, live, live)).twoMedia());
        assertFalse(ThreeTwoOne.check(List.of(new ThreeTwoOne.Copy("a", "ssd", true, true), nas, live)).oneImmutable());
    }

    @Test
    void retentionKeepsRecentDaysSundaysAndMonthStarts() {
        LocalDate today = LocalDate.of(2026, 2, 4);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 399; i >= 0; i--) {
            days.add(today.minusDays(i));
        }
        var kept = Retention.keep(days, today, 7, 4, 12);
        assertTrue(kept.contains(today));
        assertTrue(kept.contains(today.minusDays(6)));
        assertTrue(kept.contains(LocalDate.of(2025, 3, 1)));
        assertFalse(kept.contains(LocalDate.of(2025, 2, 1)));
        assertEquals(21, kept.size());
        assertTrue(kept.stream().anyMatch(d -> d.getDayOfWeek() == DayOfWeek.SUNDAY));
    }

    @Test
    void objectivesAreWorkedOutFromTheTimeline() {
        Objectives.Outcome outcome = Objectives.outcome(120, 930, 950, 95, 12);
        assertEquals(810, outcome.rpoMinutes());
        assertEquals(115, outcome.rtoMinutes());
        assertEquals(162, outcome.ordersLost());
        assertEquals("13:30", Objectives.clock(810));
    }

    @Test
    void ransomwareEncryptsWritableFilesAndIsStoppedByReadOnlyUntilTheOwnerChangesIt() throws Exception {
        Path live = BackupsDemo.shop(dir);
        byte[] before = Files.readAllBytes(live.resolve("orders.csv"));
        assertEquals(3, Ransomware.encryptInPlace(live).encrypted());
        assertFalse(java.util.Arrays.equals(before, Files.readAllBytes(live.resolve("orders.csv"))));
        Path other = dir.resolve("other");
        Files.createDirectories(other);
        Files.writeString(other.resolve("a.txt"), "a");
        Files.setPosixFilePermissions(other.resolve("a.txt"), PosixFilePermissions.fromString("r--------"));
        assertEquals(1, Ransomware.encryptInPlace(other).blocked());
        Files.setPosixFilePermissions(other.resolve("a.txt"), PosixFilePermissions.fromString("rw-------"));
        assertEquals(1, Ransomware.encryptInPlace(other).encrypted());
    }

    @Test
    void versionedStoreKeepsEveryVersionAndAnswersAsOfATime() {
        Ransomware.VersionedStore store = new Ransomware.VersionedStore();
        store.append("f", new byte[] {1}, 100);
        store.append("f", new byte[] {9}, 200);
        assertArrayEquals(new byte[] {1}, store.latestBefore("f", 199).orElseThrow());
        assertArrayEquals(new byte[] {9}, store.latestBefore("f", 200).orElseThrow());
        assertTrue(store.latestBefore("f", 99).isEmpty());
        assertTrue(store.latestBefore("g", 500).isEmpty());
        assertEquals(2, store.versionCount("f"));
        byte[] copy = store.latestBefore("f", 199).orElseThrow();
        copy[0] = 42;
        assertArrayEquals(new byte[] {1}, store.latestBefore("f", 199).orElseThrow());
    }

    @Test
    void sealedArchiveRoundTripsAndHidesItsContent() throws Exception {
        Map<String, byte[]> files = Map.of("orders.csv", "customer Ana".getBytes(StandardCharsets.UTF_8));
        byte[] key = BackupVault.newKey();
        byte[] archive = BackupVault.seal(key, "night-1", files);
        assertFalse(new String(archive, StandardCharsets.ISO_8859_1).contains("Ana"));
        assertArrayEquals(files.get("orders.csv"), BackupVault.open(key, "night-1", archive).get("orders.csv"));
    }

    @Test
    void wrongKeyAndChangedArchiveAreRefused() throws Exception {
        byte[] key = BackupVault.newKey();
        byte[] archive = BackupVault.seal(key, "night-1", Map.of("a", new byte[] {1, 2, 3}));
        assertThrows(BackupVault.WrongKeyException.class, () -> BackupVault.open(BackupVault.newKey(), "night-1", archive));
        byte[] changed = archive.clone();
        changed[changed.length - 1] ^= 1;
        assertThrows(BackupVault.WrongKeyException.class, () -> BackupVault.open(key, "night-1", changed));
        assertThrows(BackupVault.WrongKeyException.class, () -> BackupVault.open(key, "night-1", new byte[5]));
        assertThrows(BackupVault.WrongKeyException.class, () -> BackupVault.open(key, "night-2", archive));
    }

    @Test
    void keyFileIsOwnerOnly() throws IOException {
        Path file = dir.resolve("backup.key");
        BackupVault.writeKeyFile(file, BackupVault.newKey());
        assertEquals("rw-------", BackupVault.permissions(file));
        assertEquals(32, Files.readAllBytes(file).length);
    }
}
