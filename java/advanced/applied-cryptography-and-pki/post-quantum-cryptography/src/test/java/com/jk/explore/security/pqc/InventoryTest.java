package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InventoryTest {

    @Test
    void countsProjectsNotFilesAndIgnoresTestsAndBuildOutput(@TempDir Path dir) throws Exception {
        write(dir, "beginner/m/a/src/main/java/A.java", "Signature.getInstance(\"SHA256withRSA\"); KeyPairGenerator.getInstance(\"EC\");");
        write(dir, "beginner/m/a/src/main/java/B.java", "KeyPairGenerator.getInstance(\"RSA\");");
        write(dir, "beginner/m/a/src/test/java/T.java", "ECDSA ECDH X25519");
        write(dir, "intermediate/m/b/build/x/src/main/java/C.java", "ECDSA");
        write(dir, "intermediate/m/b/src/main/java/D.java", "Cipher.getInstance(\"AES/GCM/NoPadding\");");
        Inventory.Result r = Inventory.scan(dir);
        assertEquals(2, r.projects());
        assertEquals(3, r.files());
        assertEquals(1, r.projectsByAlgorithm().get("RSA").size());
        assertEquals(1, r.projectsByAlgorithm().get("EC key pair (any use)").size());
        assertEquals(0, r.projectsByAlgorithm().get("ECDSA").size());
        assertEquals(1, r.projectsByAlgorithm().get("AES").size());
        assertEquals(1, r.anyPublicKey().size());
    }

    @Test
    void findsTheRepositoryFromThisProject() {
        assertTrue(Inventory.javaRoot(Path.of("")) != null);
    }

    private static void write(Path root, String rel, String text) throws Exception {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text);
    }
}
