package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HardCodedAndFileTest {

    @TempDir
    Path dir;

    @Test
    void aHardCodedKeyIsFoundInsideTheCompiledClass() throws IOException {
        try (InputStream in = HardCodedKey.class.getResourceAsStream("HardCodedKey.class")) {
            assertTrue(Hex.contains(in.readAllBytes(), HardCodedKey.key()));
        }
    }

    @Test
    void aKeyFileWithDefaultPermissionsIsReadableByOthers() throws IOException {
        Path file = KeyFile.writeWithDefaultPermissions(dir.resolve("a.key"), Hex.bytes("k"));
        assertEquals("rw-r--r--", KeyFile.permissions(file));
        assertTrue(KeyFile.readableByOthers(file));
    }

    @Test
    void anOwnerOnlyKeyFileIsNot() throws IOException {
        Path file = KeyFile.writeOwnerOnly(dir.resolve("b.key"), Hex.bytes("k"));
        assertEquals("rw-------", KeyFile.permissions(file));
        assertFalse(KeyFile.readableByOthers(file));
    }

    @Test
    void aKeyFileHoldsTheKeyInTheClear() throws IOException {
        byte[] key = Hex.bytes("clear-key");
        Path file = KeyFile.writeOwnerOnly(dir.resolve("c.key"), key);
        assertArrayEquals(key, KeyFile.read(file));
    }

    @Test
    void theHelperFindsNeedlesAndOnlyTheNeedles() {
        assertTrue(Hex.contains(Hex.bytes("xxabcxx"), Hex.bytes("abc")));
        assertFalse(Hex.contains(Hex.bytes("xxabxcxx"), Hex.bytes("abc")));
        assertTrue(Hex.contains(Hex.bytes("abc"), Hex.bytes("abc")));
        assertFalse(Hex.contains(Hex.bytes("ab"), Hex.bytes("abc")));
    }
}
