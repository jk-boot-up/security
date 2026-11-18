package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** The blob size the demo works out: AES key wrap with padding rounds the key up to 8 bytes and adds 8. JDK only, no token. */
class KeyWrapSizeTest {

    @Test
    void sixtySevenKeyBytesWrapToEighty() throws Exception {
        Cipher kwp = Cipher.getInstance("AES/KWP/NoPadding");
        kwp.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"));
        assertEquals(80, kwp.doFinal(new byte[67]).length);
        for (int n = 1; n <= 100; n++) {
            kwp.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"));
            assertEquals((n + 7) / 8 * 8 + 8, kwp.doFinal(new byte[n]).length);
        }
    }
}
