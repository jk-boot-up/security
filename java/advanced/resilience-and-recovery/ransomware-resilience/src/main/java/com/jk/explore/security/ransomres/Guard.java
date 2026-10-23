package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The check a backup job makes before it copies anything: is the canary file untouched, and does the data still look like a ledger? */
final class Guard {

    static final String CANARY_NAME = "zz-canary-accounts.csv";

    /** What the three detectors said (true means they raised the alarm). */
    record Verdict(boolean canaryChanged, boolean fileEntropyHigh, boolean blockEntropyHigh) {

        boolean clean() {
            return !canaryChanged && !fileEntropyHigh && !blockEntropyHigh;
        }

        String detectors() {
            return "canary=" + yesNo(canaryChanged) + " file-entropy=" + yesNo(fileEntropyHigh) + " block-entropy=" + yesNo(blockEntropyHigh);
        }
    }

    private final String canaryHash;

    Guard(byte[] canary) {
        this.canaryHash = Sha.hex(canary);
    }

    Verdict check(Path dir) throws IOException {
        Path canary = dir.resolve(CANARY_NAME);
        boolean canaryChanged = !Files.isRegularFile(canary) || !Sha.hex(Files.readAllBytes(canary)).equals(canaryHash);
        boolean fileHigh = false;
        boolean blockHigh = false;
        for (Path file : Ransomware.files(dir)) {
            byte[] data = Files.readAllBytes(file);
            fileHigh |= data.length >= Entropy.BLOCK / 2 && Entropy.bits(data) > Entropy.LIMIT;
            blockHigh |= Entropy.maxBlock(data) > Entropy.LIMIT;
        }
        return new Verdict(canaryChanged, fileHigh, blockHigh);
    }

    /** The same content test applied to bytes that come back from a restore. */
    static boolean looksLikeLedger(byte[] data) {
        return Entropy.maxBlock(data) <= Entropy.LIMIT;
    }

    static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }
}
