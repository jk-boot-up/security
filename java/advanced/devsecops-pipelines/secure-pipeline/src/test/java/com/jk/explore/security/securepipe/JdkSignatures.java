package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** ECDSA signatures made with the JDK only, so that the unit tests need no cosign. The public key file holds the base64 of the encoded key. */
final class JdkSignatures implements SignatureCheck {

    record Pair(PrivateKey signing, Path publicKeyFile) {
    }

    static Pair newPair(Path dir, String name) throws GeneralSecurityException, IOException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        KeyPair kp = g.generateKeyPair();
        Path pub = dir.resolve(name + ".pub");
        Files.writeString(pub, Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        return new Pair(kp.getPrivate(), pub);
    }

    static void sign(PrivateKey key, Path file, Path signature) throws GeneralSecurityException, IOException {
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(key);
        s.update(Files.readAllBytes(file));
        Files.writeString(signature, Base64.getEncoder().encodeToString(s.sign()));
    }

    @Override
    public boolean verify(Path publicKey, Path file, Path signature) {
        try {
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readString(publicKey))));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(key);
            s.update(Files.readAllBytes(file));
            return s.verify(Base64.getDecoder().decode(Files.readString(signature)));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return false;
        }
    }
}
