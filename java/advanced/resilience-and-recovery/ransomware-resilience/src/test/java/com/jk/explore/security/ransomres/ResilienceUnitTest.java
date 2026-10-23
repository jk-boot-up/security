package com.jk.explore.security.ransomres;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The parts that need no store and no Docker. */
class ResilienceUnitTest {

    @TempDir
    Path tmp;

    @Test
    void ledgerIsTheSameEveryTime() {
        assertArrayEquals(Ledger.snapshot(3), Ledger.snapshot(3));
        assertFalse(Sha.hex(Ledger.snapshot(3)).equals(Sha.hex(Ledger.snapshot(4))));
        assertEquals(Ledger.ROWS_PER_SNAPSHOT, Ledger.rowCount(Ledger.snapshot(0)));
    }

    @Test
    void sha256OfAKnownString() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha.hex("abc"));
    }

    @Test
    void textIsLowEntropyAndEncryptedDataIsHigh() throws Exception {
        Path dir = tmp.resolve("d");
        RansomwareResilienceDemo.fill(dir);
        byte[] text = Files.readAllBytes(dir.resolve("ledger-2026-10-01.csv"));
        assertTrue(Entropy.bits(text) < 5);
        Ransomware.encryptAndDelete(dir, Set.of());
        byte[] locked = Files.readAllBytes(dir.resolve("ledger-2026-10-01.csv.locked"));
        assertTrue(Entropy.bits(locked) > 7.9);
    }

    @Test
    void ransomwareDeletesTheOriginalsAndWritesANote() throws Exception {
        Path dir = tmp.resolve("r");
        RansomwareResilienceDemo.fill(dir);
        assertEquals(9, Ransomware.encryptAndDelete(dir, Set.of()));
        assertFalse(Files.exists(dir.resolve("ledger-2026-10-01.csv")));
        assertTrue(Files.exists(dir.resolve(Ransomware.NOTE)));
        assertEquals(10, Ransomware.files(dir).size());
    }

    @Test
    void guardPassesAHealthyDirectoryAndStopsEveryKindOfEncryption() throws Exception {
        Guard guard = new Guard(Ledger.canary());
        Path dir = tmp.resolve("g");
        RansomwareResilienceDemo.fill(dir);
        assertTrue(guard.check(dir).clean());
        Ransomware.encryptAndDelete(dir, Set.of());
        Guard.Verdict full = guard.check(dir);
        assertTrue(full.canaryChanged() && full.fileEntropyHigh() && full.blockEntropyHigh());

        RansomwareResilienceDemo.fill(dir);
        Ransomware.encryptAndDelete(dir, Set.of(Guard.CANARY_NAME));
        Guard.Verdict skipped = guard.check(dir);
        assertFalse(skipped.canaryChanged());
        assertTrue(skipped.fileEntropyHigh());
    }

    @Test
    void halfEncryptionHidesFromTheWholeFileTestButNotFromTheBlockTest() throws Exception {
        Guard guard = new Guard(Ledger.canary());
        Path dir = tmp.resolve("h");
        RansomwareResilienceDemo.fill(dir);
        Ransomware.encryptHalfInPlace(dir, Set.of(Guard.CANARY_NAME));
        Guard.Verdict v = guard.check(dir);
        assertFalse(v.fileEntropyHigh());
        assertTrue(v.blockEntropyHigh());
    }

    @Test
    void anAlteredCanaryIsCaughtEvenWithLowEntropy() throws Exception {
        Guard guard = new Guard(Ledger.canary());
        Path dir = tmp.resolve("c");
        RansomwareResilienceDemo.fill(dir);
        Files.writeString(dir.resolve(Guard.CANARY_NAME), "TX1,ACC10001,100,CR\n");
        assertTrue(guard.check(dir).canaryChanged());
    }

    @Test
    void recoveryTimeIsArithmetic() {
        // 15 min + 45 min + 120 steps x 3 s + 24 x 25 GiB at 400 MiB/s (1536 s)
        assertEquals(900 + 2700 + 360 + 1536, Recovery.rtoSeconds(24, 120, 400));
        assertTrue(Recovery.rtoSeconds(24, 120, 100) > Recovery.rtoSeconds(24, 120, 400));
        assertEquals("91.6 min", Recovery.minutes(5496));
    }

    @Test
    void recoveryPointFollowsTheSchedule() {
        assertEquals(9 * 60, Recovery.lastGoodBackup(9 * 60 + 20, 60));
        assertEquals(3000, Recovery.lostTransactions(9 * 60, 10 * 60 + 40));
        assertEquals(19200, Recovery.lostTransactions(0, 10 * 60 + 40));
        assertEquals("09:05", Recovery.clock(9 * 60 + 5));
    }

    @Test
    void policiesGiveTheWriterNoDeleteAndTheReaderNoWrite() {
        String writer = Policies.writerOnly("vault");
        assertTrue(writer.contains("s3:PutObject") && !writer.contains("Delete") && !writer.contains("GetObject"));
        String reader = Policies.readerOnly("vault");
        assertTrue(reader.contains("s3:GetObjectVersion") && !reader.contains("Put") && !reader.contains("Delete"));
        assertTrue(Policies.everything().contains("s3:*"));
    }

    @Test
    void ledgerTotalsAndRowsAreComputedFromTheFile() {
        byte[] file = "TX1,ACC1,500,CR\nTX2,ACC1,200,DR\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(300, Ledger.total(file));
        assertEquals(2, Ledger.rowCount(file));
    }
}
