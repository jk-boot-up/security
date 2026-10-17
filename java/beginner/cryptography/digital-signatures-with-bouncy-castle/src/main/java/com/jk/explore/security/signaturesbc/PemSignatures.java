package com.jk.explore.security.signaturesbc;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

/** PEM armor for signatures and public keys: Base64 text between BEGIN and END lines, so they can be pasted into email or a ticket. */
final class PemSignatures {

    private PemSignatures() {
    }

    static String write(String type, byte[] der) {
        StringWriter out = new StringWriter();
        try (PemWriter writer = new PemWriter(out)) {
            writer.writeObject(new PemObject(type, der));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    static byte[] read(String type, String pem) {
        try (PemReader reader = new PemReader(new StringReader(pem))) {
            PemObject object = reader.readPemObject();
            if (object == null || !object.getType().equals(type)) {
                throw new IllegalArgumentException("expected a " + type + " block");
            }
            return object.getContent();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A public key in the SubjectPublicKeyInfo form that OpenSSL reads. */
    static String publicKey(AsymmetricKeyParameter key) {
        try {
            SubjectPublicKeyInfo info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
            return write("PUBLIC KEY", info.getEncoded());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A private key in the PKCS#8 form that OpenSSL reads, in the clear: used only by the tests, with a throw-away key. */
    static String privateKey(java.security.KeyPair keys) {
        return write("PRIVATE KEY", keys.getPrivate().getEncoded());
    }
}
