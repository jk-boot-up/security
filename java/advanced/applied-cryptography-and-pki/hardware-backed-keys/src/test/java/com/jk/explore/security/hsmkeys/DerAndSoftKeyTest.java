package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DerAndSoftKeyTest {

    @Test
    void lengthsUseTheShortAndLongForms() {
        assertArrayEquals(new byte[] {0x04, 0x01, 0x07}, Der.tlv(0x04, new byte[] {7}));
        byte[] big = Der.tlv(0x04, new byte[200]);
        assertEquals((byte) 0x81, big[1]);
        assertEquals(203, big.length);
        assertEquals((byte) 0x82, Der.tlv(0x04, new byte[300])[1]);
    }

    @Test
    void oidsAreEncodedWithBase128Arcs() {
        assertArrayEquals(new byte[] {0x06, 0x03, 0x55, 0x04, 0x03}, Der.oid(2, 5, 4, 3));
        assertArrayEquals(new byte[] {0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0B}, Der.oid(1, 2, 840, 113549, 1, 1, 11));
    }

    @Test
    void aSelfSignedCertificateBuiltByHandIsAcceptedByTheJdk() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        byte[] der = Der.selfSignedRsa("Demo Bank", "Test", kp.getPublic(), BigInteger.TEN, "260101000000Z", "360101000000Z", tbs -> {
            try {
                Signature s = Signature.getInstance("SHA256withRSA");
                s.initSign(kp.getPrivate());
                s.update(tbs);
                return s.sign();
            } catch (java.security.GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        });
        X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
        c.verify(kp.getPublic());
        assertEquals("CN=Test, O=Demo Bank", c.getSubjectX500Principal().getName("RFC1779"));
        assertEquals(BigInteger.TEN, c.getSerialNumber());
    }

    @Test
    void aKeyInAFileIsFoundByADictionaryAndCanBeReadOut(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("k.p12");
        String password = String.join("", "pass", "word", "9");
        SoftKey.create(file, password);
        assertEquals(3, SoftKey.crack(file, List.of("a", "b", password, "c")));
        assertEquals(0, SoftKey.crack(file, List.of("a", "b")));
        assertNotNull(SoftKey.open(file, password).getEncoded());
        assertTrue(Files.size(file) > 0);
    }

    @Test
    void paymentFilesAreDeterministic() {
        assertArrayEquals(PaymentFile.content("X", 3, 99), PaymentFile.content("X", 3, 99));
        assertTrue(new String(PaymentFile.content("X", 3, 99)).endsWith("TOTAL 99\n"));
    }
}
