package com.jk.explore.security.signatures;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Signs and checks messages with Ed25519. A signature is made with the private key and checked with the public key, so
 * anyone who has the public key can check but not make one. Ed25519 is deterministic: the same key and message always give
 * the same 64 bytes, with no random number involved.
 *
 * <p>Every message is signed together with a context, a short label of its purpose, so that a signature made for one purpose
 * can never be accepted for another.
 */
public final class Signing {

    public static final int SIGNATURE_BYTES = 64;

    private Signing() {
    }

    public static byte[] sign(PrivateKey key, String context, byte[] message) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update(withContext(context, message));
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Signs the message as it is, with no context. For checking against the published test vectors only. */
    public static byte[] signRaw(PrivateKey key, byte[] message) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update(message);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verify(PublicKey key, String context, byte[] message, byte[] signatureBytes) {
        return verifyRaw(key, withContext(context, message), signatureBytes);
    }

    public static boolean verifyRaw(PublicKey key, byte[] message, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(key);
            signature.update(message);
            return signature.verify(signatureBytes);
        } catch (java.security.SignatureException e) {
            return false;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The context, a zero byte, then the message: the zero byte keeps a context from running into the message. */
    static byte[] withContext(String context, byte[] message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(Hex.bytes(context));
        out.write(0);
        out.writeBytes(message);
        return out.toByteArray();
    }
}
