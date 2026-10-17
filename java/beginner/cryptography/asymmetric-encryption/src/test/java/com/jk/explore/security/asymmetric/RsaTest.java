package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RsaTest {

    private static KeyPair ana;
    private static KeyPair eve;

    @BeforeAll
    static void keys() {
        ana = RsaBox.newKeyPair();
        eve = RsaBox.newKeyPair();
    }

    @Test
    void whatIsLockedWithThePublicKeyOpensWithThePrivateKey() {
        byte[] message = Hex.bytes("a short secret");
        assertArrayEquals(message, RsaBox.unlock(ana.getPrivate(), RsaBox.lock(ana.getPublic(), message)));
    }

    @Test
    void anotherPrivateKeyCannotOpenIt() {
        byte[] locked = RsaBox.lock(ana.getPublic(), Hex.bytes("secret"));
        assertThrows(RsaBox.NotForThisKeyException.class, () -> RsaBox.unlock(eve.getPrivate(), locked));
    }

    @Test
    void theSameMessageLockedTwiceGivesDifferentBytes() {
        byte[] message = Hex.bytes("secret");
        assertFalse(Arrays.equals(RsaBox.lock(ana.getPublic(), message), RsaBox.lock(ana.getPublic(), message)));
    }

    @Test
    void theLockedMessageIsAlwaysAsLongAsTheKey() {
        assertEquals(256, RsaBox.lock(ana.getPublic(), new byte[1]).length);
        assertEquals(256, RsaBox.lock(ana.getPublic(), new byte[190]).length);
    }

    @Test
    void ahundredAndNinetyBytesFitAndOneMoreDoesNot() {
        assertEquals(190, RsaBox.maxMessageBytes());
        RsaBox.lock(ana.getPublic(), new byte[190]);
        assertThrows(IllegalArgumentException.class, () -> RsaBox.lock(ana.getPublic(), new byte[191]));
    }

    @Test
    void aPublicKeyIs294BytesWhenEncoded() {
        assertEquals(294, ana.getPublic().getEncoded().length);
    }

    @Test
    void aChangedLockedMessageDoesNotOpen() {
        byte[] locked = RsaBox.lock(ana.getPublic(), Hex.bytes("secret"));
        locked[10] ^= 1;
        assertThrows(RsaBox.NotForThisKeyException.class, () -> RsaBox.unlock(ana.getPrivate(), locked));
    }
}
