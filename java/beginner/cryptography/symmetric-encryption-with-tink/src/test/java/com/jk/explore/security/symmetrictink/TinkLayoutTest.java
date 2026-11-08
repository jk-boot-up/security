package com.jk.explore.security.symmetrictink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** The hand-built layout is exactly Tink's: Tink opens it, and a real Tink message splits into the same parts. */
class TinkLayoutTest {

    private final Bank bank = new Bank(DemoKeysets.aes256Gcm(TinkDemo.KEY_ID, TinkDemo.KEY_TEXT));

    @Test
    void thePrefixIsVersionOneThenTheKeyId() {
        assertEquals("01000003e9", HexFormat.of().formatHex(TinkLayout.prefix(1001)));
    }

    @Test
    void tinkOpensTheHandBuiltMessage() throws Exception {
        byte[] message = TinkDemo.byHand();
        assertEquals(49, message.length);
        assertArrayEquals(TinkDemo.LINE, bank.openOrNull(TinkDemo.ACCOUNT, message));
    }

    @Test
    void aRealTinkMessageHasTheSameParts() throws Exception {
        byte[] real = bank.seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        TinkLayout.Parts parts = TinkLayout.split(real);
        assertArrayEquals(TinkLayout.prefix(TinkDemo.KEY_ID), parts.prefix());
        // Rebuilding it by hand with Tink's own random nonce gives the very same bytes.
        assertArrayEquals(real, TinkLayout.seal(DemoKeysets.keyBytes(TinkDemo.KEY_TEXT), TinkDemo.KEY_ID,
                parts.nonce(), TinkDemo.ACCOUNT, TinkDemo.LINE));
        // And plain JDK AES-GCM opens the nonce, cipher text and tag.
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DemoKeysets.keyBytes(TinkDemo.KEY_TEXT), "AES"),
                new GCMParameterSpec(128, parts.nonce()));
        cipher.updateAAD(TinkDemo.ACCOUNT);
        byte[] body = new byte[parts.cipherText().length + parts.tag().length];
        System.arraycopy(parts.cipherText(), 0, body, 0, parts.cipherText().length);
        System.arraycopy(parts.tag(), 0, body, parts.cipherText().length, parts.tag().length);
        assertArrayEquals(TinkDemo.LINE, cipher.doFinal(body));
    }

    @Test
    void rotationAddsAFixedPrimaryKeyAndKeepsTheOldOne() throws Exception {
        var rotated = TinkDemo.rotate(DemoKeysets.aes256Gcm(TinkDemo.KEY_ID, TinkDemo.KEY_TEXT));
        assertEquals(2, rotated.size());
        assertEquals(TinkDemo.NEW_KEY_ID, rotated.getPrimary().getId());
        byte[] fresh = new Bank(rotated).seal(TinkDemo.ACCOUNT, TinkDemo.LINE);
        assertArrayEquals(TinkLayout.prefix(TinkDemo.NEW_KEY_ID), TinkLayout.split(fresh).prefix());
    }
}
