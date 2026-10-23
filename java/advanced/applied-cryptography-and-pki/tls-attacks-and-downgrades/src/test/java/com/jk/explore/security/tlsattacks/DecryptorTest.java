package com.jk.explore.security.tlsattacks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DecryptorTest {

    static KeyPair server;
    static KeyPair other;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        server = g.generateKeyPair();
        other = g.generateKeyPair();
    }

    @Test
    void theTls12PrfMatchesAPublishedTestVector() throws Exception {
        HexFormat hex = HexFormat.of();
        byte[] out = Decryptor.prf(hex.parseHex("9bbe436ba940f017b17652849a71db35"), "test label", hex.parseHex("a0ba9f936cda311827a6f796ffd5198c"), 100);
        assertEquals("e3f229ba727be17b8d122620557cd453c2aab21d07c3d495329b52d4e61edb5a6b301791e90d35c9c9a46b4e14baf9af0fa022f7077def17abfd3797c0564bab4fbc91666e9def9b97fce34f796789baa48082d122ee42c5a72e5a5110fff70187347b66",
                hex.formatHex(out));
    }

    /** Builds what a client would send for a static RSA, AES-128-GCM TLS 1.2 session. */
    static byte[][] session(String text, boolean rsaExchange) throws Exception {
        SecureRandom rnd = new SecureRandom();
        byte[] cr = new byte[32];
        byte[] sr = new byte[32];
        rnd.nextBytes(cr);
        rnd.nextBytes(sr);
        byte[] premaster = new byte[48];
        rnd.nextBytes(premaster);
        premaster[0] = 3;
        premaster[1] = 3;
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, server.getPublic());
        byte[] encrypted = rsaExchange ? rsa.doFinal(premaster) : new byte[65];
        ByteArrayOutputStream cke = new ByteArrayOutputStream();
        int bodyLen = encrypted.length + (rsaExchange ? 2 : 1);
        cke.write(16);
        cke.write(0);
        cke.write(bodyLen >> 8);
        cke.write(bodyLen);
        if (rsaExchange) {
            cke.write(encrypted.length >> 8);
            cke.write(encrypted.length);
        } else {
            cke.write(encrypted.length);
        }
        cke.writeBytes(encrypted);

        byte[] master = Decryptor.prf(premaster, "master secret", Decryptor.concat(cr, sr), 48);
        byte[] block = Decryptor.prf(master, "key expansion", Decryptor.concat(sr, cr), 40);
        byte[] key = Arrays.copyOfRange(block, 0, 16);
        byte[] salt = Arrays.copyOfRange(block, 32, 36);

        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        byte[] explicit = new byte[8];
        rnd.nextBytes(explicit);
        Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
        gcm.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Decryptor.concat(salt, explicit)));
        gcm.updateAAD(ByteBuffer.allocate(13).putLong(0).put((byte) 23).putShort((short) 0x0303).putShort((short) data.length).array());
        byte[] sealed = Decryptor.concat(explicit, gcm.doFinal(data));

        ByteArrayOutputStream up = new ByteArrayOutputStream();
        up.writeBytes(TlsRecordsTest.record(22, 0x0303, hello(1, cr)));
        up.writeBytes(TlsRecordsTest.record(22, 0x0303, cke.toByteArray()));
        up.writeBytes(TlsRecordsTest.record(20, 0x0303, new byte[] {1}));
        up.writeBytes(TlsRecordsTest.record(23, 0x0303, sealed));
        ByteArrayOutputStream down = new ByteArrayOutputStream();
        down.writeBytes(TlsRecordsTest.record(22, 0x0303, hello(2, sr)));
        return new byte[][] {up.toByteArray(), down.toByteArray()};
    }

    static byte[] hello(int type, byte[] random) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(3);
        b.write(3);
        b.writeBytes(random);
        b.write(0);
        b.write(0);
        b.write(0x9c);
        b.write(0);
        byte[] body = b.toByteArray();
        ByteArrayOutputStream m = new ByteArrayOutputStream();
        m.write(type);
        m.write(0);
        m.write(body.length >> 8);
        m.write(body.length);
        m.writeBytes(body);
        return m.toByteArray();
    }

    @Test
    void theLeakedKeyOpensAStaticRsaSession() throws Exception {
        byte[][] s = session("POST /transfer amount=2500", true);
        assertEquals(Optional.of("POST /transfer amount=2500"), Decryptor.clientData(s[0], s[1], server.getPrivate()));
    }

    @Test
    void anotherKeyOpensNothing() throws Exception {
        byte[][] s = session("POST /transfer amount=2500", true);
        assertTrue(Decryptor.clientData(s[0], s[1], other.getPrivate()).isEmpty());
    }

    @Test
    void aKeyExchangeThatIsNotRsaGivesTheKeyNothingToDecrypt() throws Exception {
        byte[][] s = session("POST /transfer amount=2500", false);
        assertTrue(Decryptor.clientData(s[0], s[1], server.getPrivate()).isEmpty());
    }

    @Test
    void aRecordingWithoutAKeyExchangeGivesNothing() throws Exception {
        assertTrue(Decryptor.clientData(new byte[0], new byte[0], server.getPrivate()).isEmpty());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Decryptor.concat(new byte[] {1, 2}, new byte[] {3, 4}));
    }
}
