package com.jk.explore.security.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeaksTest {

    @TempDir
    Path dir;

    @Test
    void scannerFindsNamedSettingsAndRandomValues() throws Exception {
        List<SecretScanner.Finding> findings = SecretScanner.scan(SecretsDemo.writeSampleRepository(dir));
        assertEquals(3, findings.size());
        assertTrue(findings.stream().allMatch(f -> !f.file().contains("production")));
    }

    @Test
    void scannerIgnoresPlaceholdersAndShortOrderlyValues() {
        assertTrue(SecretScanner.scanText("a", "db.password=${DB_PASSWORD}\npayment.api-key=file:/run/secrets/k").isEmpty());
        assertTrue(SecretScanner.scanText("a", "build.id=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").isEmpty());
        assertEquals(1, SecretScanner.scanText("a", "x=" + SecretsDemo.randomLookingToken()).size());
    }

    @Test
    void entropyIsMeasuredInBitsPerCharacter() {
        assertEquals(0.0, SecretScanner.entropy("aaaa"), 1e-9);
        assertEquals(1.0, SecretScanner.entropy("abab"), 1e-9);
        assertEquals(2.0, SecretScanner.entropy("abcd"), 1e-9);
    }

    @Test
    void aFileDeletedInALaterLayerIsStillInTheImage() throws Exception {
        ImageLayers image = new ImageLayers();
        image.addLayer(Map.of("app/a.key", "demo"));
        image.deleteInNewLayer("app/a.key");
        assertFalse(image.mergedView().containsKey("app/a.key"));
        assertEquals("demo", image.layer(0).get("app/a.key"));
    }

    @Test
    void secretFilesReadableByOthersAreRefused() throws Exception {
        Path file = dir.resolve("s");
        MountedSecrets.write(file, "demo-secret\n", "rw-r--r--");
        assertThrows(MountedSecrets.UnsafeFileException.class, () -> MountedSecrets.read(file));
        MountedSecrets.write(file, "demo-secret\n", "rw-------");
        Secret secret = MountedSecrets.read(file);
        assertTrue(secret.matches("demo-secret".toCharArray()));
    }
}
