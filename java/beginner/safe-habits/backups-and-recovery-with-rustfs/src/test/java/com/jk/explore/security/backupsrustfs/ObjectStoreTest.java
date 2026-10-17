package com.jk.explore.security.backupsrustfs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;

/** These tests start a real object store in Docker: run them with ./gradlew integrationTest. */
@Tag("docker")
class ObjectStoreTest {

    @Test
    void anOverwriteKeepsTheOldVersionAndItCanBeRestored() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("versions", false);
            root.enableVersioning("versions");
            String first = root.put("versions", "k", Store.bytes("one"));
            root.put("versions", "k", Store.bytes("two"));
            assertEquals(2, root.versionIds("versions", "k").size());
            assertArrayEquals(Store.bytes("one"), root.getVersion("versions", "k", first));
            assertArrayEquals(Store.bytes("two"), root.get("versions", "k"));
        }
    }

    @Test
    void aPlainDeleteOnlyAddsAMarker() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("versions", false);
            root.enableVersioning("versions");
            String first = root.put("versions", "k", Store.bytes("one"));
            root.delete("versions", "k");
            assertEquals(1, root.deleteMarkerCount("versions", "k"));
            assertTrue(Store.outcome(() -> root.get("versions", "k")).startsWith("refused (404"));
            assertArrayEquals(Store.bytes("one"), root.getVersion("versions", "k", first));
        }
    }

    @Test
    void aComplianceLockRefusesEveryDeleteOfTheVersion() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("locked", true);
            String v = root.put("locked", "k", Store.bytes("keep"));
            root.retain("locked", "k", v, ObjectLockRetentionMode.COMPLIANCE, Instant.now().plus(1, ChronoUnit.DAYS), false);
            assertEquals("refused (403 AccessDenied)", Store.outcome(() -> root.deleteVersion("locked", "k", v, false)));
            assertEquals("refused (403 AccessDenied)", Store.outcome(() -> root.deleteVersion("locked", "k", v, true)));
            assertEquals("refused (403 AccessDenied)", Store.outcome(() -> root.retain("locked", "k", v,
                    ObjectLockRetentionMode.COMPLIANCE, Instant.now().plus(1, ChronoUnit.HOURS), true)));
            assertArrayEquals(Store.bytes("keep"), root.getVersion("locked", "k", v));
        }
    }

    @Test
    void aGovernanceLockCanBeBypassedOnlyOnRequest() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("locked", true);
            String v = root.put("locked", "k", Store.bytes("keep"));
            root.retain("locked", "k", v, ObjectLockRetentionMode.GOVERNANCE, Instant.now().plus(1, ChronoUnit.DAYS), false);
            assertEquals("refused (403 AccessDenied)", Store.outcome(() -> root.deleteVersion("locked", "k", v, false)));
            assertEquals("allowed", Store.outcome(() -> root.deleteVersion("locked", "k", v, true)));
        }
    }

    @Test
    void theWriteOnlyIdentityCanOnlyAddObjectsToItsBucket() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("backups", true);
            root.createBucket("other", false);
            c.createWriteOnlyUser("writer", "writer-secret-1", "backups");
            try (Store writer = new Store(c.endpoint(), "writer", "writer-secret-1")) {
                assertEquals("allowed", Store.outcome(() -> writer.put("backups", "n", Store.bytes("n"))));
                assertEquals("refused (403 AccessDenied)", Store.outcome(() -> writer.get("backups", "n")));
                assertEquals("refused (403 AccessDenied)", Store.outcome(() -> writer.delete("backups", "n")));
                assertEquals("refused (403 AccessDenied)", Store.outcome(() -> writer.versionIds("backups", "n")));
                assertEquals("refused (403 AccessDenied)", Store.outcome(() -> writer.put("other", "n", Store.bytes("n"))));
            }
        }
    }

    @Test
    void aWrongChecksumIsRefusedAndLeavesNothingBehind() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("checks", false);
            root.enableVersioning("checks");
            assertEquals("allowed", Store.outcome(() -> root.put("checks", "good", Store.bytes("data"))));
            assertEquals("refused (400 BadDigest)", Store.outcome(() -> root.putWithChecksumOf("checks", "bad", Store.bytes("data"), Store.bytes("other"))));
            assertTrue(root.versionIds("checks", "bad").isEmpty());
        }
    }

    @Test
    void lifecycleRuleIsStoredAndReadBack() throws Exception {
        try (ObjectStoreContainer c = ObjectStoreContainer.start(); Store root = Store.root(c)) {
            root.createBucket("life", false);
            root.enableVersioning("life");
            root.expireOldVersionsAfter("life", 30);
            assertEquals(30, root.lifecycleRules("life").get(0).noncurrentVersionExpiration().noncurrentDays());
        }
    }

    @Test
    void closingRemovesTheContainerAndTheNetwork() throws Exception {
        String before = ObjectStoreContainer.run(java.util.List.of("docker", "ps", "-a", "--format", "{{.Names}}"));
        try (ObjectStoreContainer c = ObjectStoreContainer.start()) {
            assertTrue(ObjectStoreContainer.run(java.util.List.of("docker", "ps", "--format", "{{.Names}}")).contains("backups-store-"));
        }
        String after = ObjectStoreContainer.run(java.util.List.of("docker", "ps", "-a", "--format", "{{.Names}}"));
        assertEquals(before.contains("backups-store-"), after.contains("backups-store-"));
        assertEquals(false, ObjectStoreContainer.run(java.util.List.of("docker", "network", "ls", "--format", "{{.Name}}")).contains("backups-net-"));
    }
}
