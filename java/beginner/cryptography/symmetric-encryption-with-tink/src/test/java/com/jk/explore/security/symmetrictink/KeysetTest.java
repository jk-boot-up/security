package com.jk.explore.security.symmetrictink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") // Tink 1.24 deprecates its encrypted-keyset functions; see TinkDemo.act4KeysetOnDisk
class KeysetTest {

    private KeysetHandle original() {
        return DemoKeysets.aes256Gcm(TinkDemo.KEY_ID, TinkDemo.KEY_TEXT);
    }

    private KeysetHandle rotated(KeysetHandle from) throws GeneralSecurityException {
        return KeysetHandle.newBuilder(from)
                .addEntry(KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary())
                .build();
    }

    @Test
    void theDemoKeysetHasOneEnabledPrimaryKeyWithTheGivenId() {
        KeysetHandle keyset = original();
        assertEquals(1, keyset.size());
        assertEquals(TinkDemo.KEY_ID, keyset.getPrimary().getId());
    }

    @Test
    void afterRotationOldMessagesStillOpenAndNewOnesUseTheNewKey() throws Exception {
        byte[] old = new Bank(original()).seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        KeysetHandle rotated = rotated(original());
        Bank bank = new Bank(rotated);
        byte[] fresh = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        assertEquals(2, rotated.size());
        assertArrayEquals(TinkDemo.LINE, bank.openOrNull(TinkDemo.ACCOUNT, old));
        assertNotEquals(Arrays.toString(Arrays.copyOf(old, 5)), Arrays.toString(Arrays.copyOf(fresh, 5)));
    }

    @Test
    void removingTheOldKeyMakesOldMessagesUnreadable() throws Exception {
        byte[] old = new Bank(original()).seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        KeysetHandle rotated = rotated(original());
        byte[] fresh = new Bank(rotated).seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        KeysetHandle onlyNew = KeysetHandle.newBuilder(rotated).deleteAt(0).build();
        assertNull(new Bank(onlyNew).openOrNull(TinkDemo.ACCOUNT, old));
        assertArrayEquals(TinkDemo.LINE, new Bank(onlyNew).openOrNull(TinkDemo.ACCOUNT, fresh));
    }

    @Test
    void aKeysetWrittenEncryptedHidesTheKeyAndReadsBackWithTheMasterKey() throws Exception {
        Aead master = new Bank(DemoKeysets.aes256Gcm(2001, "demo-master-key-standing-for-a-kms")).aead();
        String json = TinkJsonProtoKeysetFormat.serializeEncryptedKeyset(original(), master, new byte[0]);
        assertTrue(json.contains("encryptedKeyset"));
        assertFalse(json.contains(DemoKeysets.keyBase64(TinkDemo.KEY_TEXT)));
        byte[] sealed = new Bank(original()).seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        KeysetHandle back = TinkJsonProtoKeysetFormat.parseEncryptedKeyset(json, master, new byte[0]);
        assertArrayEquals(TinkDemo.LINE, new Bank(back).openOrNull(TinkDemo.ACCOUNT, sealed));
    }

    @Test
    void aKeysetWrittenEncryptedDoesNotReadBackWithAnotherMasterKey() throws Exception {
        Aead master = new Bank(DemoKeysets.aes256Gcm(2001, "demo-master-key-standing-for-a-kms")).aead();
        String json = TinkJsonProtoKeysetFormat.serializeEncryptedKeyset(original(), master, new byte[0]);
        Aead wrong = new Bank(DemoKeysets.aes256Gcm(2001, "a-different-master-key-text-here")).aead();
        assertThrows(GeneralSecurityException.class, () -> TinkJsonProtoKeysetFormat.parseEncryptedKeyset(json, wrong, new byte[0]));
    }

    @Test
    void thereIsAnAuthenticatedTemplateButNoEcbOne() throws Exception {
        assertEquals("AES256_GCM", "AES256_GCM");
        KeyTemplates.get("AES256_GCM");
        assertThrows(GeneralSecurityException.class, () -> KeyTemplates.get("AES128_ECB"));
    }
}
