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
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

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
    void envelopeEncryptionWrapsADifferentDataKeyForEveryMessage() throws Exception {
        Aead envelope = TinkDemo.envelope(new Bank(DemoKeysets.aes256Gcm(2001, TinkDemo.MASTER_TEXT)).aead());
        byte[] a = envelope.encrypt(TinkDemo.LINE, TinkDemo.ACCOUNT);
        byte[] b = envelope.encrypt(TinkDemo.LINE, TinkDemo.ACCOUNT);
        assertFalse(Arrays.equals(TinkDemo.wrappedKey(a), TinkDemo.wrappedKey(b)));
        assertArrayEquals(TinkDemo.LINE, envelope.decrypt(a, TinkDemo.ACCOUNT));
    }

    @Test
    void theMasterKeyIsUsedOncePerMessageWhateverItsSize() throws Exception {
        CountingAead master = new CountingAead(new Bank(DemoKeysets.aes256Gcm(2001, TinkDemo.MASTER_TEXT)).aead());
        Aead envelope = TinkDemo.envelope(master);
        envelope.encrypt(new byte[10], TinkDemo.ACCOUNT);
        envelope.encrypt(new byte[1_000_000], TinkDemo.ACCOUNT);
        assertEquals(2, master.calls());
    }

    @Test
    void aDifferentMasterKeyCannotOpenAnEnvelopeMessage() throws Exception {
        byte[] message = TinkDemo.envelope(new Bank(DemoKeysets.aes256Gcm(2001, TinkDemo.MASTER_TEXT)).aead())
                .encrypt(TinkDemo.LINE, TinkDemo.ACCOUNT);
        Aead other = TinkDemo.envelope(new Bank(DemoKeysets.aes256Gcm(2001, "a-different-master-key-text-here")).aead());
        assertThrows(GeneralSecurityException.class, () -> other.decrypt(message, TinkDemo.ACCOUNT));
    }

    @Test
    void aChangedWrappedKeyOrCipherTextOrAccountIsRefused() throws Exception {
        Aead envelope = TinkDemo.envelope(new Bank(DemoKeysets.aes256Gcm(2001, TinkDemo.MASTER_TEXT)).aead());
        byte[] message = envelope.encrypt(TinkDemo.LINE, TinkDemo.ACCOUNT);
        byte[] wrappedChanged = message.clone();
        wrappedChanged[10] ^= 1;
        byte[] cipherChanged = message.clone();
        cipherChanged[cipherChanged.length - 1] ^= 1;
        assertThrows(GeneralSecurityException.class, () -> envelope.decrypt(wrappedChanged, TinkDemo.ACCOUNT));
        assertThrows(GeneralSecurityException.class, () -> envelope.decrypt(cipherChanged, TinkDemo.ACCOUNT));
        assertThrows(GeneralSecurityException.class, () -> envelope.decrypt(message, "account=MAL-2".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theMasterKeysOwnBytesNeverAppearInAStoredMessage() throws Exception {
        byte[] message = TinkDemo.envelope(new Bank(DemoKeysets.aes256Gcm(2001, TinkDemo.MASTER_TEXT)).aead())
                .encrypt(TinkDemo.LINE, TinkDemo.ACCOUNT);
        byte[] masterBytes = DemoKeysets.keyBytes(TinkDemo.MASTER_TEXT);
        boolean found = false;
        for (int i = 0; i + masterBytes.length <= message.length; i++) {
            found |= Arrays.equals(Arrays.copyOfRange(message, i, i + masterBytes.length), masterBytes);
        }
        assertFalse(found);
    }

    @Test
    void thereIsAnAuthenticatedTemplateButNoEcbOne() throws Exception {
        assertEquals("AES256_GCM", "AES256_GCM");
        KeyTemplates.get("AES256_GCM");
        assertThrows(GeneralSecurityException.class, () -> KeyTemplates.get("AES128_ECB"));
    }
}
