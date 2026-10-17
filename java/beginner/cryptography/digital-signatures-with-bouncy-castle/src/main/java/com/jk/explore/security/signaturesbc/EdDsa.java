package com.jk.explore.security.signaturesbc;

import java.security.SecureRandom;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.signers.Ed448Signer;

/** Ed25519 and Ed448 (RFC 8032): deterministic signatures, 64 and 114 bytes, with no nonce to get wrong. */
final class EdDsa {

    private EdDsa() {
    }

    static byte[] sign25519(byte[] seed, byte[] message) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, new Ed25519PrivateKeyParameters(seed, 0));
        signer.update(message, 0, message.length);
        return signer.generateSignature();
    }

    static boolean verify25519(byte[] publicKey, byte[] message, byte[] signature) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, new Ed25519PublicKeyParameters(publicKey, 0));
        signer.update(message, 0, message.length);
        return signer.verifySignature(signature);
    }

    static byte[] public25519(byte[] seed) {
        return new Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().getEncoded();
    }

    static byte[] sign448(byte[] seed, byte[] message) {
        Ed448Signer signer = new Ed448Signer(new byte[0]);
        signer.init(true, new Ed448PrivateKeyParameters(seed, 0));
        signer.update(message, 0, message.length);
        return signer.generateSignature();
    }

    static boolean verify448(byte[] publicKey, byte[] message, byte[] signature) {
        Ed448Signer signer = new Ed448Signer(new byte[0]);
        signer.init(false, new Ed448PublicKeyParameters(publicKey, 0));
        signer.update(message, 0, message.length);
        return signer.verifySignature(signature);
    }

    static byte[] public448(byte[] seed) {
        return new Ed448PrivateKeyParameters(seed, 0).generatePublicKey().getEncoded();
    }

    static byte[] newSeed25519() {
        byte[] seed = new byte[32];
        new SecureRandom().nextBytes(seed);
        return seed;
    }
}
