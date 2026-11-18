package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A teaching class: the check cosign verify-blob makes, redone with the JDK alone. cosign.pub is a PEM public key, the .sig file is the base64 of an ECDSA
 * signature over the SHA-256 of the file's bytes. A test proves it agrees with the signatures the demo checks.
 */
final class SignatureSteps implements SignatureCheck {

    /** The public key from a PEM file (cosign.pub) or from a file that holds only its base64. */
    static PublicKey publicKey(Path file) throws IOException, GeneralSecurityException {
        String pem = Files.readString(file).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    /** "EC, 256 bits": the key's algorithm and the size of its curve. */
    static String describe(Path publicKeyFile) throws IOException, GeneralSecurityException {
        PublicKey key = publicKey(publicKeyFile);
        return key.getAlgorithm() + ", " + ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize() + " bits";
    }

    @Override
    public boolean verify(Path publicKey, Path file, Path signature) {
        try {
            // Step 1 and 2 in one object: SHA256withECDSA hashes the bytes with SHA-256 and checks the ECDSA signature of that digest.
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(publicKey(publicKey));
            s.update(Files.readAllBytes(file));
            return s.verify(Base64.getDecoder().decode(Files.readString(signature).strip()));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return false;
        }
    }
}
