package com.jk.explore.security.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessagesTest {

    private KeystoreVault vault() {
        return KeystoreVault.create(KeystoreDemo.PASSWORD)
                .put("statements-v1", Hex.bytes("keystore-statement-key-32-bytes!"))
                .put("statements-v2", Hex.bytes("a-newer-statement-key-32-bytes!!"));
    }

    @Test
    void aSealedMessageNamesItsKeyAndOpens() {
        KeystoreVault vault = vault();
        byte[] message = Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE);
        assertEquals("statements-v1", Messages.keyNameOf(message));
        assertArrayEquals(KeystoreDemo.ACCOUNT_NOTE, Messages.open(vault, message));
    }

    @Test
    void oldAndNewMessagesBothOpenWhileBothKeysAreInTheVault() {
        KeystoreVault vault = vault();
        byte[] old = Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE);
        byte[] fresh = Messages.seal(vault, "statements-v2", KeystoreDemo.ACCOUNT_NOTE);
        assertArrayEquals(KeystoreDemo.ACCOUNT_NOTE, Messages.open(vault, old));
        assertArrayEquals(KeystoreDemo.ACCOUNT_NOTE, Messages.open(vault, fresh));
    }

    @Test
    void aMessageWhoseKeyWasRemovedCannotBeOpened() {
        KeystoreVault vault = vault();
        byte[] old = Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE);
        vault.remove("statements-v1");
        assertThrows(Messages.CannotOpenException.class, () -> Messages.open(vault, old));
    }

    @Test
    void aChangedMessageOrKeyNameIsRefused() {
        KeystoreVault vault = vault();
        byte[] message = Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE);
        byte[] flipped = message.clone();
        flipped[flipped.length - 1] ^= 1;
        assertThrows(Messages.CannotOpenException.class, () -> Messages.open(vault, flipped));
        // the key name is bound to the message as associated data: pointing it at v2 fails to open
        byte[] pointedAtV2 = Messages.seal(vault, "statements-v2", KeystoreDemo.ACCOUNT_NOTE);
        System.arraycopy(Hex.bytes("statements-v1"), 0, pointedAtV2, 1, 13);
        assertThrows(Messages.CannotOpenException.class, () -> Messages.open(vault, pointedAtV2));
    }

    @Test
    void theSameNoteSealedTwiceGivesDifferentMessages() {
        KeystoreVault vault = vault();
        assertEquals(false, java.util.Arrays.equals(Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE),
                Messages.seal(vault, "statements-v1", KeystoreDemo.ACCOUNT_NOTE)));
    }
}
