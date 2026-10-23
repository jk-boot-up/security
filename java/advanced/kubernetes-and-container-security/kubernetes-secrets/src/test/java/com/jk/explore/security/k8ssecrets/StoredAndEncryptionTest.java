package com.jk.explore.security.k8ssecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class StoredAndEncryptionTest {

    @Test
    void base64IsUndoneByAnyoneWithNoKey() {
        String encoded = Base64.getEncoder().encodeToString("demo-card-vault-key-001".getBytes());
        assertEquals("demo-card-vault-key-001", new String(Base64.getDecoder().decode(encoded)));
    }

    @Test
    void aValueWithTheMarkerIsEncryptedAndOneWithoutIsNot() {
        assertTrue(new Stored("k8s:enc:aescbc:v1:key1:\u0001\u0002").encrypted());
        assertFalse(new Stored("k8s\0\n\u000bv1 secret demo").encrypted());
        assertTrue(new Stored("secret demo").contains("demo"));
        assertTrue(new Stored("  \n").absent());
    }

    @Test
    void theKeyIs256BitsAndDiffersEveryTime() {
        String key = Encryption.newKey();
        assertEquals(32, Base64.getDecoder().decode(key).length);
        assertNotEquals(key, Encryption.newKey());
    }

    @Test
    void theConfigurationEncryptsFirstAndStillReadsWhatWasStored() {
        String config = Encryption.aescbc("KEY");
        assertTrue(config.contains("secret: KEY"));
        assertTrue(config.indexOf("aescbc") < config.indexOf("identity"), "the first provider encrypts new writes");
        assertFalse(Encryption.identityOnly().contains("aescbc"));
    }
}
