package com.jk.explore.security.pqc;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KEM;

/** Measures the byte sizes that a protocol has to carry. Every number comes from a real key, ciphertext or signature produced here. */
final class Sizes {

    /** What a key-establishment algorithm puts on the wire: a public key one way and a message back. */
    record Exchange(String name, int publicKey, int reply, int secret) {
    }

    /** What a signature algorithm puts on the wire. */
    record Signing(String name, int publicKey, int signature) {
    }

    private Sizes() {
    }

    static List<Exchange> exchanges() throws GeneralSecurityException {
        List<Exchange> rows = new ArrayList<>();
        int x = KeyPairGenerator.getInstance("X25519").generateKeyPair().getPublic().getEncoded().length;
        rows.add(new Exchange("X25519", x, x, 32));

        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(new ECGenParameterSpec("secp256r1"));
        int p = ec.generateKeyPair().getPublic().getEncoded().length;
        rows.add(new Exchange("ECDH P-256", p, p, 32));

        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(3072);
        KeyPair r = rsa.generateKeyPair();
        Cipher oaep = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaep.init(Cipher.ENCRYPT_MODE, r.getPublic());
        rows.add(new Exchange("RSA-3072 key transport", r.getPublic().getEncoded().length, oaep.doFinal(new byte[32]).length, 32));

        for (String set : new String[] {"ML-KEM-512", "ML-KEM-768", "ML-KEM-1024"}) {
            KeyPair kp = KeyPairGenerator.getInstance(set).generateKeyPair();
            KEM.Encapsulated e = KEM.getInstance("ML-KEM").newEncapsulator(kp.getPublic()).encapsulate();
            rows.add(new Exchange(set, kp.getPublic().getEncoded().length, e.encapsulation().length, e.key().getEncoded().length));
        }
        return rows;
    }

    static List<Signing> signings() throws GeneralSecurityException {
        byte[] message = new byte[100];
        List<Signing> rows = new ArrayList<>();
        rows.add(measure("Ed25519", KeyPairGenerator.getInstance("Ed25519"), Signature.getInstance("Ed25519"), message));

        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(new ECGenParameterSpec("secp256r1"));
        // The fixed-length encoding (r and s side by side); the usual DER encoding is 70 to 72 bytes.
        rows.add(measure("ECDSA P-256", ec, Signature.getInstance("SHA256withECDSAinP1363Format"), message));

        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(3072);
        rows.add(measure("RSA-3072 PKCS#1", rsa, Signature.getInstance("SHA256withRSA"), message));

        for (String set : new String[] {"ML-DSA-44", "ML-DSA-65", "ML-DSA-87"}) {
            rows.add(measure(set, KeyPairGenerator.getInstance(set), Signature.getInstance("ML-DSA"), message));
        }
        return rows;
    }

    private static Signing measure(String name, KeyPairGenerator generator, Signature signature, byte[] message) throws GeneralSecurityException {
        KeyPair kp = generator.generateKeyPair();
        signature.initSign(kp.getPrivate());
        signature.update(message);
        return new Signing(name, kp.getPublic().getEncoded().length, signature.sign().length);
    }

    static Exchange exchange(List<Exchange> rows, String name) {
        return rows.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    static Signing signing(List<Signing> rows, String name) {
        return rows.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    /** Key and signature bytes in a chain of two certificates plus the handshake signature: 2 public keys and 3 signatures. */
    static int chainBytes(Signing s) {
        return 2 * s.publicKey() + 3 * s.signature();
    }

    /** A ratio with one decimal, as text, from integers (no floating point in the output). */
    static String times(int big, int small) {
        int tenths = Math.round(10f * big / small);
        return (tenths / 10) + "." + (tenths % 10);
    }
}
