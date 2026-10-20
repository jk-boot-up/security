package com.jk.explore.security.tlslog;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.XECPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Makes one connection between a client and a server, in the two ways TLS has made keys: an ephemeral Diffie-Hellman exchange (X25519, as in TLS 1.3),
 * or a secret chosen by the client and encrypted under the server's long-term RSA key (static RSA key exchange, removed in TLS 1.3).
 * To keep the demo's output repeatable, "random" values come from a labelled generator. Real code uses {@link SecureRandom} only.
 */
public final class Connection {

    /** What the client and the server both know after the handshake. */
    public record Established(Capture capture, Secrets secrets) {
    }

    private Connection() {
    }

    static Established ecdhe(String label, List<String> clientSends, List<String> serverSends) {
        try {
            SecureRandom r = new LabelledRandom(label);
            byte[] cr = bytes(r, 32);
            byte[] sr = bytes(r, 32);
            KeyPairGenerator g = KeyPairGenerator.getInstance("X25519");
            g.initialize(NamedParameterSpec.X25519, new LabelledRandom(label + "/client"));
            KeyPair client = g.generateKeyPair();
            g.initialize(NamedParameterSpec.X25519, new LabelledRandom(label + "/server"));
            KeyPair server = g.generateKeyPair();
            byte[] shared = agree(client.getPrivate(), server.getPublic());
            byte[] cpub = raw(client.getPublic());
            byte[] spub = raw(server.getPublic());
            Secrets secrets = Secrets.derive(shared, Hkdf.sha256(cr, cpub, sr, spub));
            return new Established(new Capture(label, true, cr, sr, cpub, spub, records(secrets, clientSends, serverSends)), secrets);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static Established staticRsa(String label, PublicKey serverLongTermKey, List<String> clientSends, List<String> serverSends) {
        try {
            SecureRandom r = new LabelledRandom(label);
            byte[] cr = bytes(r, 32);
            byte[] sr = bytes(r, 32);
            byte[] premaster = bytes(r, 32);
            Cipher c = Cipher.getInstance("RSA/ECB/OAEPPadding");
            c.init(Cipher.ENCRYPT_MODE, serverLongTermKey, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] encrypted = c.doFinal(premaster);
            Secrets secrets = Secrets.derive(premaster, Hkdf.sha256(cr, encrypted, sr));
            return new Established(new Capture(label, false, cr, sr, encrypted, new byte[0], records(secrets, clientSends, serverSends)), secrets);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Packet> records(Secrets s, List<String> clientSends, List<String> serverSends) {
        List<Packet> out = new ArrayList<>();
        for (int i = 0; i < clientSends.size(); i++) {
            out.add(new Packet(true, Records.seal(s.client(), i, clientSends.get(i))));
        }
        for (int i = 0; i < serverSends.size(); i++) {
            out.add(new Packet(false, Records.seal(s.server(), i, serverSends.get(i))));
        }
        return out;
    }

    static byte[] agree(PrivateKey priv, PublicKey pub) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(priv);
        ka.doPhase(pub, true);
        return ka.generateSecret();
    }

    static PublicKey publicKeyFromRaw(byte[] raw) throws GeneralSecurityException {
        byte[] be = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            be[i] = raw[raw.length - 1 - i];
        }
        return KeyFactory.getInstance("X25519").generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, new BigInteger(1, be)));
    }

    static byte[] raw(PublicKey k) {
        byte[] be = ((XECPublicKey) k).getU().toByteArray();
        byte[] le = new byte[32];
        for (int i = 0; i < be.length && i < 33; i++) {
            if (be.length - 1 - i < 32 && be.length - 1 - i >= 0 && i < 32) {
                le[i] = be[be.length - 1 - i];
            }
        }
        return le;
    }

    private static byte[] bytes(SecureRandom r, int n) {
        byte[] b = new byte[n];
        r.nextBytes(b);
        return b;
    }

}
