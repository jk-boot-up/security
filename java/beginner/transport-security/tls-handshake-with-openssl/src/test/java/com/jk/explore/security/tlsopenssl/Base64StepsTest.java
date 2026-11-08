package com.jk.explore.security.tlsopenssl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Base64 by hand must give the JDK's exact text. */
class Base64StepsTest {

    @Test
    void theCertificatesFirstThreeBytesAreMiib() throws Exception {
        byte[] der = TestCertificates.certificate("shop.p12").getEncoded();
        assertArrayEquals(new int[] {12, 8, 8, 1}, Base64Steps.groups(Arrays.copyOf(der, 3)));
        assertEquals("MIIB", Base64Steps.encode(Arrays.copyOf(der, 3)));
    }

    @Test
    void everyGroupOfThreeBytesMatchesTheJdk() throws Exception {
        byte[] der = TestCertificates.certificate("shop.p12").getEncoded();
        StringBuilder byHand = new StringBuilder();
        for (int i = 0; i + 3 <= der.length; i += 3) {
            byHand.append(Base64Steps.encode(Arrays.copyOfRange(der, i, i + 3)));
        }
        String jdk = Base64.getEncoder().encodeToString(der);
        assertEquals(jdk.substring(0, byHand.length()), byHand.toString());
    }

    @Test
    void binaryIsPadded() {
        assertEquals("000001", Base64Steps.binary(1, 6));
        assertEquals("10000010", Base64Steps.binary(0x82, 8));
    }
}
