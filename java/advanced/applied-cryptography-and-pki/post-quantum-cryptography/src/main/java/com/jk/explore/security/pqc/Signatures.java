package com.jk.explore.security.pqc;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

/** ML-DSA signing and verifying through the ordinary java.security API. */
final class Signatures {

    private Signatures() {
    }

    static byte[] sign(KeyPair pair, byte[] message) throws GeneralSecurityException {
        Signature s = Signature.getInstance("ML-DSA");
        s.initSign(pair.getPrivate());
        s.update(message);
        return s.sign();
    }

    static boolean verify(KeyPair pair, byte[] message, byte[] signature) throws GeneralSecurityException {
        Signature s = Signature.getInstance("ML-DSA");
        s.initVerify(pair.getPublic());
        s.update(message);
        return s.verify(signature);
    }

    static KeyPair generate(String parameterSet) throws GeneralSecurityException {
        return KeyPairGenerator.getInstance(parameterSet).generateKeyPair();
    }

    static byte[] statement() {
        return "statement 2031-04 account 4417: closing balance 1200.00".getBytes(StandardCharsets.UTF_8);
    }
}
