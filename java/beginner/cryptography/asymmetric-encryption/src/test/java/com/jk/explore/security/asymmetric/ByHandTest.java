package com.jk.explore.security.asymmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import org.junit.jupiter.api.Test;

/** The by-hand maths gives the tiny numbers the demo shows, and exactly what Java's own RSA and DH give on real keys. */
class ByHandTest {

    @Test
    void tinyRsaKeysAndLock() {
        ByHand.RsaKeys keys = ByHand.rsaKeys(89, 97, 17);
        assertEquals(8633, keys.n());
        assertEquals(497, keys.d());
        assertEquals(4485, ByHand.lock(keys, 4821));
        assertEquals(4821, ByHand.open(keys, 4485));
    }

    @Test
    void everyTinyMessageComesBack() {
        ByHand.RsaKeys keys = ByHand.rsaKeys(89, 97, 17);
        for (long m = 0; m < keys.n(); m++) {
            assertEquals(m, ByHand.open(keys, ByHand.lock(keys, m)));
        }
    }

    @Test
    void tinyKeyAgreementGivesTheSameSecret() {
        long bankSends = ByHand.power(5, 6, 23);
        long anaSends = ByHand.power(5, 15, 23);
        assertEquals(8, bankSends);
        assertEquals(19, anaSends);
        assertEquals(2, ByHand.power(anaSends, 6, 23));
        assertEquals(2, ByHand.power(bankSends, 15, 23));
    }

    @Test
    void byHandRsaIsWhatJavaRsaDoesWithoutPadding() throws Exception {
        KeyPair pair = RsaBox.newKeyPair();
        RSAPublicKey pub = (RSAPublicKey) pair.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) pair.getPrivate();
        BigInteger m = new BigInteger(1, Hex.bytes("PIN is 4821"));
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        BigInteger c = new BigInteger(1, cipher.doFinal(m.toByteArray()));
        assertEquals(ByHand.power(m, pub.getPublicExponent(), pub.getModulus()), c);
        assertEquals(m, ByHand.power(c, priv.getPrivateExponent(), priv.getModulus()));
    }

    @Test
    void byHandKeyAgreementIsWhatJavaDhDoes() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        generator.initialize(2048);
        KeyPair bank = generator.generateKeyPair();
        DHPublicKey bankPublic = (DHPublicKey) bank.getPublic();
        KeyPairGenerator anaGenerator = KeyPairGenerator.getInstance("DH");
        anaGenerator.initialize(bankPublic.getParams());
        KeyPair ana = anaGenerator.generateKeyPair();
        BigInteger p = bankPublic.getParams().getP();
        BigInteger g = bankPublic.getParams().getG();
        BigInteger x = ((DHPrivateKey) bank.getPrivate()).getX();
        assertEquals(bankPublic.getY(), ByHand.power(g, x, p));
        KeyAgreement agreement = KeyAgreement.getInstance("DH");
        agreement.init(bank.getPrivate());
        agreement.doPhase(ana.getPublic(), true);
        assertEquals(ByHand.power(((DHPublicKey) ana.getPublic()).getY(), x, p), new BigInteger(1, agreement.generateSecret()));
    }

    @Test
    void fixedX25519KeysKeepTheirRawBytes() {
        assertEquals(AsymmetricDemo.BANK_PUBLIC, Hex.of(KeyAgreementBox.raw(KeyAgreementBox.publicKey(Hex.parse(AsymmetricDemo.BANK_PUBLIC)))));
        assertEquals(AsymmetricDemo.ANA_PRIVATE, Hex.of(KeyAgreementBox.raw(KeyAgreementBox.privateKey(Hex.parse(AsymmetricDemo.ANA_PRIVATE)))));
    }
}
