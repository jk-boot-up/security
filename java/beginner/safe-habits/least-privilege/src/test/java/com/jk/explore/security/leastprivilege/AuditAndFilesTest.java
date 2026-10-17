package com.jk.explore.security.leastprivilege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditAndFilesTest {

    @TempDir
    Path dir;

    @Test
    void permissionsUnusedForMoreThanTheLimitAreFlagged() {
        var recent = new UsageAudit.Holding("a", "p", 110);
        var exactly = new UsageAudit.Holding("a", "q", 110);
        var old = new UsageAudit.Holding("a", "r", 109);
        var never = new UsageAudit.Holding("a", "s", -1);
        assertEquals(List.of(old, never), UsageAudit.unused(List.of(recent, exactly, old, never), 200, 90));
    }

    @Test
    void nothingIsFlaggedWhenEverythingIsUsed() {
        assertTrue(UsageAudit.unused(List.of(new UsageAudit.Holding("a", "p", 199)), 200, 90).isEmpty());
    }

    @Test
    void lockedFilesAreOwnerOnly() throws IOException {
        Path file = Files.writeString(dir.resolve("f"), "x");
        assertEquals("rw-r--r--", OwnerOnly.openFile(file));
        assertTrue(OwnerOnly.readableByOthers(file));
        assertEquals("rw-------", OwnerOnly.lockFile(file));
        assertFalse(OwnerOnly.readableByOthers(file));
        assertEquals("rwx------", OwnerOnly.lockDirectory(dir));
    }
}
