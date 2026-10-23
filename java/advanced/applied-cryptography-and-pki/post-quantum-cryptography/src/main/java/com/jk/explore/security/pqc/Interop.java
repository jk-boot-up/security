package com.jk.explore.security.pqc;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KEM;
import javax.crypto.KeyGenerator;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Two independent implementations, the JDK's and Bouncy Castle's, exchanging public keys, ciphertexts and signatures. */
final class Interop {

    private static final BouncyCastleProvider BC = new BouncyCastleProvider();

    private Interop() {
    }

    /** The JDK encapsulates to a key made by Bouncy Castle; Bouncy Castle decapsulates. True when both hold the same secret. */
    static boolean jdkEncapsulatesBouncyCastleDecapsulates() throws GeneralSecurityException {
        KeyPair bc = KeyPairGenerator.getInstance("ML-KEM-768", BC).generateKeyPair();
        PublicKey asJdk = KeyFactory.getInstance("ML-KEM").generatePublic(new X509EncodedKeySpec(bc.getPublic().getEncoded()));
        KEM.Encapsulated e = KEM.getInstance("ML-KEM").newEncapsulator(asJdk).encapsulate();
        KeyGenerator extract = KeyGenerator.getInstance("ML-KEM", BC);
        extract.init(new KEMExtractSpec.Builder(bc.getPrivate(), e.encapsulation(), "AES", 256).withNoKdf().build());
        return Arrays.equals(extract.generateKey().getEncoded(), e.key().getEncoded());
    }

    /** Bouncy Castle encapsulates to a key made by the JDK; the JDK decapsulates. */
    static boolean bouncyCastleEncapsulatesJdkDecapsulates() throws GeneralSecurityException {
        KeyPair jdk = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        PublicKey asBc = KeyFactory.getInstance("ML-KEM", BC).generatePublic(new X509EncodedKeySpec(jdk.getPublic().getEncoded()));
        KeyGenerator generate = KeyGenerator.getInstance("ML-KEM", BC);
        generate.init(new KEMGenerateSpec.Builder(asBc, "AES", 256).withNoKdf().build());
        SecretKeyWithEncapsulation bcKey = (SecretKeyWithEncapsulation) generate.generateKey();
        byte[] jdkKey = KEM.getInstance("ML-KEM").newDecapsulator(jdk.getPrivate()).decapsulate(bcKey.getEncapsulation()).getEncoded();
        return Arrays.equals(jdkKey, bcKey.getEncoded());
    }

    /** The JDK signs, Bouncy Castle verifies with the key re-read from its X.509 bytes. */
    static boolean jdkSignsBouncyCastleVerifies(byte[] message) throws GeneralSecurityException {
        KeyPair jdk = Signatures.generate("ML-DSA-65");
        byte[] signature = Signatures.sign(jdk, message);
        PublicKey asBc = KeyFactory.getInstance("ML-DSA", BC).generatePublic(new X509EncodedKeySpec(jdk.getPublic().getEncoded()));
        Signature verify = Signature.getInstance("ML-DSA", BC);
        verify.initVerify(asBc);
        verify.update(message);
        return verify.verify(signature);
    }

    /** Bouncy Castle signs, the JDK verifies. */
    static boolean bouncyCastleSignsJdkVerifies(byte[] message) throws GeneralSecurityException {
        KeyPair bc = KeyPairGenerator.getInstance("ML-DSA-65", BC).generateKeyPair();
        Signature sign = Signature.getInstance("ML-DSA", BC);
        sign.initSign(bc.getPrivate());
        sign.update(message);
        byte[] signature = sign.sign();
        PublicKey asJdk = KeyFactory.getInstance("ML-DSA").generatePublic(new X509EncodedKeySpec(bc.getPublic().getEncoded()));
        Signature verify = Signature.getInstance("ML-DSA");
        verify.initVerify(asJdk);
        verify.update(message);
        return verify.verify(signature);
    }

    /** The two public key encodings have the same length and the JDK re-encodes a Bouncy Castle key to identical bytes. */
    static boolean publicKeyEncodingsAgree() throws GeneralSecurityException {
        KeyPair bc = KeyPairGenerator.getInstance("ML-KEM-768", BC).generateKeyPair();
        PublicKey asJdk = KeyFactory.getInstance("ML-KEM").generatePublic(new X509EncodedKeySpec(bc.getPublic().getEncoded()));
        return Arrays.equals(asJdk.getEncoded(), bc.getPublic().getEncoded());
    }

    /** Whether the JDK accepts a Bouncy Castle private key encoding. They differ, so only public keys, ciphertexts and signatures cross. */
    static boolean jdkReadsBouncyCastlePrivateKey() throws GeneralSecurityException {
        KeyPair bc = KeyPairGenerator.getInstance("ML-KEM-768", BC).generateKeyPair();
        try {
            KeyFactory.getInstance("ML-KEM").generatePrivate(new PKCS8EncodedKeySpec(bc.getPrivate().getEncoded()));
            return true;
        } catch (java.security.spec.InvalidKeySpecException e) {
            return false;
        }
    }
}
